/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.auto.ui.fragment.components

import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.R
import net.ankio.auto.databinding.CardRuleVersionBinding
import net.ankio.auto.http.api.CategoryMapAPI
import net.ankio.auto.http.api.RuleManageAPI
import net.ankio.auto.http.api.SettingAPI
import net.ankio.auto.http.license.RuleAPI
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.storage.ZipUtils
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.utils.LoadingUtils
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager

import net.ankio.auto.utils.UpdateModel
import net.ankio.auto.utils.VersionUtils
import org.ezbook.server.constant.DataType
import org.ezbook.server.constant.Setting
import org.ezbook.server.db.model.CategoryMapModel
import org.ezbook.server.db.model.RuleModel

/**
 * 规则版本卡片组件
 *
 * 负责管理规则版本的显示、检查和更新功能。该组件提供以下主要功能：
 * 1. 显示当前规则版本和最后更新时间
 * 2. 检查规则更新并提示用户
 * 3. 下载并安装新的规则包
 * 4. 同步分类映射数据
 *
 * @param binding 卡片视图绑定对象
 */
class RuleVersionCardComponent(
    binding: CardRuleVersionBinding
) : BaseComponent<CardRuleVersionBinding>(binding) {


    /**
     * 初始化组件
     * 设置UI样式、事件监听器和自动检查更新
     */
    override fun onComponentCreate() {
        super.onComponentCreate()
        // 设置卡片背景颜色为Material Design的表面颜色
        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))

        // 设置更新按钮点击事件
        binding.updateButton.setOnClickListener {
            launch {
                updateRules(true)
            }
        }

        // 长按更新按钮重置版本号并强制更新
        binding.updateButton.setOnLongClickListener {
            PrefManager.ruleVersion = ""
            launch {
                updateRules(true)
            }
            true
        }

        // 如果开启了自动检查更新，则执行更新检查
        if (PrefManager.autoCheckRuleUpdate) {
            launch {
                updateRules(false)
            }
        }
    }

    /**
     * 组件恢复时更新显示内容
     */
    override fun onComponentResume() {
        super.onComponentResume()
        updateDisplay()
    }

    /**
     * 更新显示内容
     * 显示当前规则版本和最后更新时间
     */
    private fun updateDisplay() {
        binding.titleText.text =
            context.getString(R.string.rule_version_title, PrefManager.ruleVersion)
        binding.subtitleText.text =
            context.getString(R.string.rule_version_last_update, PrefManager.ruleUpdate)
    }

    /**
     * 设置更新按钮的启用状态
     *
     * @param enabled 是否启用按钮
     */
    private fun setUpdateButtonEnabled(enabled: Boolean) {
        binding.updateButton.isEnabled = enabled
        binding.updateButton.alpha = if (enabled) 1.0f else 0.5f
    }

    /**
     * 检查并更新规则
     * 从服务器获取最新版本信息，如果有更新则显示更新对话框
     */
    private suspend fun updateRules(fromUser: Boolean) {
        setUpdateButtonEnabled(false)
        if (fromUser) {
            ToastUtils.info(R.string.check_update)
        }
        try {
            // 获取服务器最新版本信息
            val json = RuleAPI.lastVer()
            val update = try {
                VersionUtils.fromJSON(json)
            } catch (e: NullPointerException) {
                val channel = PrefManager.appChannel
                CacheManager.remove("app_version_$channel")
                null
            }
            if (update == null) {
                if (fromUser) {
                    ToastUtils.error(R.string.no_need_to_update)
                }
                setUpdateButtonEnabled(true)
                return
            }

            // 检查版本是否需要更新
            if (!VersionUtils.isCloudVersionNewer(PrefManager.ruleVersion, update.version)) {
                if (fromUser) {
                    ToastUtils.error(R.string.no_need_to_update)
                }
                setUpdateButtonEnabled(true)
                return
            }

            // 显示更新对话框
            BaseSheetDialog.create<UpdateDialog>(context)
                .setUpdateModel(update)
                .setRuleTitle(context.getString(R.string.rule))
                .setOnClickUpdate {
                    launch {
                        updateRule(update)
                        setUpdateButtonEnabled(true)
                        updateDisplay()
                    }
                }.show()
        } finally {
            setUpdateButtonEnabled(true)
        }
    }

    /**
     * 执行规则更新操作
     *
     * @param updateModel 更新模型，包含版本信息和下载地址
     */
    private suspend fun updateRule(updateModel: UpdateModel) = withContext(Dispatchers.IO) {
        var loading: LoadingUtils? = null

        withMain {
            loading = LoadingUtils(context).apply {
                show(context.getString(R.string.downloading))
            }
        }
        try {
            // 下载规则包
            val file = context.cacheDir.resolve("rule.zip")
            if (!RuleAPI.download(updateModel.version, file)) {
                withMain { ToastUtils.error(context.getString(R.string.net_error_msg_rule)) }
                return@withContext
            }

            val zipDir = context.cacheDir.resolve("rule")

            // 判断当前版本是否大于免费版本，决定是否需要密码解压
            val passwd = if (VersionUtils.isCloudVersionNewer("v0.5.6", updateModel.version)) {
                PrefManager.token
            } else null

            // 解压规则包
            ZipUtils.unzip(file.absolutePath, zipDir.absolutePath, passwd) { filename ->
                loading?.setText(
                    context.getString(
                        R.string.unzip,
                        filename.substringAfterLast("/")
                    )
                )
            }
            loading?.setText(context.getString(R.string.unzip_finish))
            file.delete()

            // 更新规则和分类
            val distDir = zipDir.resolve("home/runner/work/AutoRule/AutoRule/dist")
            updateRulesFromDist(distDir, loading)
            updateCategoriesFromDist(distDir, loading)

            // 更新本地版本信息
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

    /**
     * 从解压目录更新规则到数据库
     *
     * @param distDir 解压后的规则目录
     * @param loading 加载提示工具
     */
    private suspend fun updateRulesFromDist(distDir: java.io.File, loading: LoadingUtils?) {
        // 读取规则配置文件
        val rulesFile = distDir.resolve("rules.json")
        val rulesArray = Gson().fromJson(rulesFile.readText(), JsonArray::class.java)

        // 更新通用JS脚本
        SettingAPI.set(Setting.JS_COMMON, distDir.resolve("common.js").readText())
        SettingAPI.set(Setting.JS_CATEGORY, distDir.resolve("category.js").readText())

        // 遍历并更新每个规则
        rulesArray.forEach { json ->
            val jsonObj = json.asJsonObject
            val ruleType = jsonObj.get("ruleType").asString
            val ruleName = jsonObj.get("ruleName").asString
            val ruleChineseName = jsonObj.get("ruleChineseName").asString
            val ruleApp = jsonObj.get("ruleApp").asString
            val jsPath = jsonObj.get("path").asString

            // 创建规则模型
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

            // 如果规则已存在，保留用户设置
            RuleManageAPI.system(ruleModel.name)?.let { sysRule ->
                ruleModel.id = sysRule.id
                ruleModel.autoRecord = sysRule.autoRecord
                ruleModel.enabled = sysRule.enabled
            }

            // 显示更新进度
            loading?.setText(
                context.getString(
                    if (ruleModel.id == 0) R.string.add_rule else R.string.update_rule,
                    ruleModel.name
                )
            )
            RuleManageAPI.put(ruleModel)
        }

        // 删除不再存在的系统规则
        loading?.setText(context.getString(R.string.delete_rule))
        RuleManageAPI.deleteSystemRule()
    }

    /**
     * 从解压目录同步分类映射
     *
     * @param distDir 解压后的规则目录
     * @param loading 加载提示工具
     */
    private suspend fun updateCategoriesFromDist(distDir: java.io.File, loading: LoadingUtils?) {
        // 获取数据库中的分类映射
        val dbCategories = CategoryMapAPI.list(1, 0)
        // 读取新版本中的分类映射
        val distCategories =
            Gson().fromJson(distDir.resolve("category.json").readText(), JsonArray::class.java)

        // 增加不存在的分类
        distCategories.forEach { json ->
            val name = json.asString
            if (dbCategories.none { it.name == name }) {
                CategoryMapAPI.put(CategoryMapModel().apply {
                    this.name = name; this.mapName = name
                })
                loading?.setText(context.getString(R.string.add_category_map, name))
            }
        }

        // 删除多余的分类
        dbCategories.forEach { dbCategory ->
            if (distCategories.none { it.asString == dbCategory.name }) {
                CategoryMapAPI.remove(dbCategory.id)
                loading?.setText(context.getString(R.string.delete_category_map, dbCategory.name))
            }
        }
    }


}
