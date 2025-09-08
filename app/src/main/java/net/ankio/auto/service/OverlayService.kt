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

package net.ankio.auto.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import net.ankio.auto.R
import net.ankio.auto.autoApp
import net.ankio.auto.service.overlay.BillWindowManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.utils.ToastUtils
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.intent.BillInfoIntent
import androidx.core.net.toUri
import net.ankio.auto.service.api.ICoreService
import net.ankio.auto.service.api.IService
import net.ankio.auto.utils.BillTool

/**
 * 悬浮窗服务（OverlayService）。
 *
 * 职责：
 * - 管理账单相关的悬浮窗显示与更新。
 * - 处理来自核心服务分发的账单意图，新增或合并展示。
 * - 在重复账单且用户允许时提示重复信息。
 *
 * 权限：
 * - 依赖系统悬浮窗权限（SYSTEM_ALERT_WINDOW）。
 * - 本服务不主动申请权限，由 [hasPermission] 与 [startPermissionActivity] 提供检测与跳转。
 */
class OverlayService : ICoreService() {

    /** 悬浮窗窗口控制器，负责具体的视图生命周期与渲染。 */
    private lateinit var billWindowManager: BillWindowManager


    /**
     * 提供底层 [CoreService] 引用，供窗口组件在需要时获取上下文或服务。
     */
    fun service() = coreService

    /**
     * 初始化悬浮窗管理器。
     *
     * 注意：此处不触发权限申请，仅完成组件构造；权限在使用前检查。
     *
     * @param coreService 绑定的核心服务实例
     */
    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)
        billWindowManager = BillWindowManager(this)

    }

    /**
     * 销毁时释放窗口资源，避免内存泄漏或窗口残留。
     */
    override fun onDestroy() {
        billWindowManager.destroy()
    }

    /**
     * 处理外部传入的账单意图。
     *
     * 行为：
     * - 若为重复账单且用户允许显示重复提示（[PrefManager.showDuplicatedPopup]），则弹出提示。
     * - 若包含父账单（merge），则更新当前悬浮窗的账单；否则新增一条悬浮窗展示。
     * - 日志仅记录关键路径，避免噪声。
     *
     * @param intent 启动或传递的意图
     * @param flags 服务启动标志
     * @param startId 唯一启动ID
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        if (intent == null) return
        val floatIntent = BillInfoIntent.parse(intent) ?: return
        val parent = floatIntent.parent
        if (parent != null) {
            if (PrefManager.showDuplicatedPopup) {
                //说明是重复账单
                ToastUtils.info(coreService.getString(R.string.repeat_bill))
            }
            billWindowManager.updateCurrentBill(parent)
            Logger.d("Repeat Bill, Parent: $parent")
        } else {
            if (floatIntent.billInfoModel.auto) {
                Logger.d("自动记录账单")
                BillTool.saveBill(floatIntent.billInfoModel)
            } else {
                billWindowManager.addBill(floatIntent.billInfoModel)
            }

        }
    }

    companion object : IService {
        /**
         * 检查是否已具备系统悬浮窗权限。
         * @return true 表示已授权；false 表示尚未授权。
         */
        override fun hasPermission(): Boolean {
            return Settings.canDrawOverlays(autoApp)
        }

        /**
         * 跳转至系统悬浮窗权限设置页面。
         *
         * 注意：该页面为系统实现，可能因 ROM 差异而表现不同。
         * @param context 上下文（将以 NEW_TASK 启动设置页面）。
         */
        override fun startPermissionActivity(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

    }
}