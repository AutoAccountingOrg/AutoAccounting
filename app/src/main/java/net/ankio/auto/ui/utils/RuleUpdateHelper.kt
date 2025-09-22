package net.ankio.auto.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.R
import net.ankio.auto.http.api.CategoryMapAPI
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.http.license.RuleAPI
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.ZipUtils
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.utils.CoroutineUtils.launchOnMain
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.UpdateModel
import net.ankio.auto.utils.VersionUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.RuleModel

/**
 * 规则更新工具类：负责检查并执行规则更新。
 *
 * 职责：
 * - 检查云端规则版本
 * - 展示更新对话框
 * - 下载并应用规则包（含规则与分类映射）
 */
object RuleUpdateHelper {

    /** 是否允许自动检查规则更新 */
    fun isAutoCheckEnabled(): Boolean = PrefManager.autoCheckRuleUpdate

    /**
     * 检查规则更新，如有更新则弹窗并在用户确认后执行更新。
     *
     * @param context 用于展示 UI
     * @param fromUser 是否用户主动触发（决定提示文案）
     * @param onUpdated 更新完成后的回调（用于刷新 UI）
     */
    suspend fun checkAndShow(context: Context, fromUser: Boolean, onUpdated: (() -> Unit)? = null) {
        if (fromUser) {
            ToastUtils.info(R.string.check_update)
        }

        val update = runCatching {
            val json = RuleAPI.lastVer(PrefManager.ruleVersion.isEmpty())
            try {
                VersionUtils.fromJSON(json)
            } catch (_: NullPointerException) {
                val channel = PrefManager.appChannel
                CacheManager.remove("app_version_${channel}")
                null
            }
        }.getOrNull()

        if (update == null) {
            if (fromUser) ToastUtils.error(R.string.no_need_to_update)
            return
        }

        if (!VersionUtils.isCloudVersionNewer(PrefManager.ruleVersion, update.version)) {
            if (fromUser) ToastUtils.error(R.string.no_need_to_update)
            return
        }

        withMain {
            BaseSheetDialog.create<UpdateDialog>(context)
                .setUpdateModel(update)
                .setRuleTitle(context.getString(R.string.rule))
                .setOnClickUpdate {
                    launchOnMain {
                        updateRule(context, update)
                        withMain { onUpdated?.invoke() }
                    }
                }
                .show()
        }
    }

    /**
     * 执行规则更新：下载、解压、落库并更新本地版本信息。
     */
    private suspend fun updateRule(context: Context, updateModel: UpdateModel) =
        withContext(Dispatchers.IO) {
            var loading: LoadingUtils? = null
            withMain {
                loading = LoadingUtils(context).apply {
                    show(context.getString(R.string.downloading))
                }
            }
            try {
                val file = context.cacheDir.resolve("rule.zip")
                if (!RuleAPI.download(updateModel.version, file)) {
                    withMain { ToastUtils.error(context.getString(R.string.net_error_msg_rule)) }
                    return@withContext
                }

                val zipDir = context.cacheDir.resolve("rule")
                val passwd = if (VersionUtils.isCloudVersionNewer("v0.5.6", updateModel.version)) {
                    PrefManager.token
                } else null

                ZipUtils.unzip(file.absolutePath, zipDir.absolutePath, passwd) { filename ->
                    loading?.setText(
                        context.getString(R.string.unzip, filename.substringAfterLast("/"))
                    )
                }
                loading?.setText(context.getString(R.string.unzip_finish))
                file.delete()

                val distDir = if (VersionUtils.isCloudVersionNewer("v0.5.6", updateModel.version)) {
                    zipDir
                } else {
                    zipDir.resolve("home/runner/work/AutoRule/AutoRule/dist")
                }

                updateRulesFromDist(distDir, loading, context)
                updateCategoriesFromDist(distDir, loading, context)

                PrefManager.ruleVersion = updateModel.version
                PrefManager.ruleUpdate = updateModel.date
                withMain { ToastUtils.info(R.string.update_success) }
            } catch (e: Exception) {
                Logger.e("更新规则错误", e)
                withMain { ToastUtils.error(R.string.update_error) }
            } finally {
                withMain { loading?.close() }
            }
        }

    /** 从 dist 目录写入规则到数据库 */
    private suspend fun updateRulesFromDist(
        distDir: java.io.File,
        loading: LoadingUtils?,
        context: Context
    ) {
        val rulesFile = distDir.resolve("rules.json")
        val rulesArray = Gson().fromJson(rulesFile.readText(), JsonArray::class.java)

        SettingAPI.set(Setting.JS_COMMON, distDir.resolve("common.js").readText())
        SettingAPI.set(Setting.JS_CATEGORY, distDir.resolve("category.js").readText())

        rulesArray.forEach { json ->
            val jsonObj = json.asJsonObject
            val ruleType = jsonObj.get("ruleType").asString
            val ruleName = jsonObj.get("ruleName").asString
            val ruleChineseName = jsonObj.get("ruleChineseName").asString
            val ruleApp = jsonObj.get("ruleApp").asString
            val jsPath = jsonObj.get("path").asString

            val ruleModel = RuleModel().apply {
                name = ruleChineseName
                systemRuleName = ruleName
                app = ruleApp
                type = when (ruleType) {
                    "app", "helper" -> DataType.DATA.name
                    "notice" -> DataType.NOTICE.name
                    else -> ""
                }
                js = distDir.resolve(jsPath).readText()
                creator = "system"
                updateAt = System.currentTimeMillis()
            }

            RuleManageAPI.system(ruleModel.name)?.let { sysRule ->
                ruleModel.id = sysRule.id
                ruleModel.autoRecord = sysRule.autoRecord
                ruleModel.enabled = sysRule.enabled
            }

            loading?.setText(
                context.getString(
                    if (ruleModel.id == 0) R.string.add_rule else R.string.update_rule,
                    ruleModel.name
                )
            )
            RuleManageAPI.put(ruleModel)
        }

        loading?.setText(context.getString(R.string.delete_rule))
        RuleManageAPI.deleteSystemRule()
    }

    /** 从 dist 目录同步分类映射 */
    private suspend fun updateCategoriesFromDist(
        distDir: java.io.File,
        loading: LoadingUtils?,
        context: Context
    ) {
        val dbCategories = CategoryMapAPI.list(1, 0)
        val distCategories =
            Gson().fromJson(distDir.resolve("category.json").readText(), JsonArray::class.java)

        distCategories.forEach { json ->
            val name = json.asString
            if (dbCategories.none { it.name == name }) {
                CategoryMapAPI.put(CategoryMapModel().apply {
                    this.name = name; this.mapName = name
                })
                loading?.setText(context.getString(R.string.add_category_map, name))
            }
        }

        dbCategories.forEach { dbCategory ->
            if (distCategories.none { it.asString == dbCategory.name }) {
                CategoryMapAPI.remove(dbCategory.id)
                loading?.setText(context.getString(R.string.delete_category_map, dbCategory.name))
            }
        }
    }
} 