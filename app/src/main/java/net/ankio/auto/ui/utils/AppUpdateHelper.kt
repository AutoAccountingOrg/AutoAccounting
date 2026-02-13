package net.ankio.auto.ui.utils

import android.content.Context
import androidx.core.net.toUri
import net.ankio.auto.BuildConfig
import net.ankio.auto.R
import net.ankio.auto.http.license.AppAPI
import net.ankio.auto.storage.CacheManager
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.dialog.UpdateDialog
import net.ankio.auto.utils.CoroutineUtils.withIO
import net.ankio.auto.utils.CoroutineUtils.withMain
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.utils.VersionUtils

/**
 * 应用更新工具类：负责检查并展示应用升级信息。
 *
 * 设计目标：
 * 1. 保持调用方简单，仅需传递 [Context] 和是否来自用户触发的标记。
 * 2. UI 相关操作在主线程执行，网络与解析在 IO 线程执行。
 * 3. 按既有行为：
 *    - 用户触发：显示“检查更新”提示；无更新时提示“无需更新”。
 *    - 后台触发：静默检查，仅在有更新时弹出对话框。
 */
object AppUpdateHelper {

    /**
     * 是否允许自动检查更新（遵循用户设置）。
     */
    fun isAutoCheckEnabled(): Boolean = PrefManager.autoCheckAppUpdate

    /**
     * 构建 APK 下载链接（遵循现有云端路径规范）。
     * 云端目录使用首字母大写的渠道名（如 Stable、Beta、Alpha）。
     *
     * @param version 云端返回的新版本号
     */
    fun buildApkUrl(version: String): String {
        val channel = PrefManager.appChannel.replaceFirstChar { it.uppercase() }
        return "https://cloud.ankio.net/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E8%87%AA%E5%8A%A8%E8%AE%B0%E8%B4%A6/%E7%89%88%E6%9C%AC%E6%9B%B4%E6%96%B0/$channel/$version.apk"
    }

    /**
     * 检查应用更新并根据结果向用户反馈。
     *
     * @param context 用于展示 UI 的上下文
     * @param fromUser 是否来自用户手动触发
     */
    suspend fun checkAndShow(context: Context, fromUser: Boolean) {
        if (fromUser) {
            withMain { ToastUtils.info(R.string.check_update) }
        }

        val update = withIO {
            val json = AppAPI.lastVer()
            try {
                VersionUtils.fromJSON(json)
            } catch (_: NullPointerException) {
                val channel = PrefManager.appChannel
                CacheManager.remove("app_version_${channel}")
                null
            }
        }

        if (update == null) {
            if (fromUser) {
                withMain { ToastUtils.error(R.string.no_need_to_update) }
            }
            return
        }

        if (!VersionUtils.isCloudVersionNewer(BuildConfig.VERSION_NAME, update.version)) {
            if (fromUser) {
                withMain { ToastUtils.error(R.string.no_need_to_update) }
            }
            return
        }

        withMain {
            BaseSheetDialog.create<UpdateDialog>(context)
                .setUpdateModel(update)
                .setRuleTitle(context.getString(R.string.app))
                .setOnClickUpdate {
                    CustomTabsHelper.launchUrl(buildApkUrl(update.version).toUri())
                }
                .show()
        }
    }
} 