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

package net.ankio.auto.utils

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.service.CoreService
import net.ankio.auto.service.ocr.ProjectionGateway
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.IntroPagerAdapter
import net.ankio.auto.ui.api.BaseActivity

/**
 * 服务管理器 - 简化版
 * 负责启动核心服务，处理OCR模式下的屏幕录制权限
 */
class ServiceManager private constructor() {

    /** 屏幕录制权限启动器 */
    private var projectionLauncher: ActivityResultLauncher<Unit>? = null

    /** 当前的就绪回调 */
    private var currentOnReady: (() -> Unit)? = null

    /** 当前的拒绝回调 */
    private var currentOnDenied: (() -> Unit)? = null

    /**
     * 提前注册屏幕录制权限启动器
     * 在Activity的onCreate中调用，避免运行时注册导致的问题
     * @param caller Activity或Fragment实例
     */
    fun registerProjectionLauncher(caller: ActivityResultCaller) {
        if (PrefManager.workMode != WorkMode.Ocr) return
        if (projectionLauncher != null) return

        projectionLauncher = ProjectionGateway.register(
            caller = caller,
            onReady = {
                currentOnReady?.invoke()
                clearCallbacks()
            },
            onDenied = {
                currentOnDenied?.invoke()
                clearCallbacks()
            }
        )
    }

    /**
     * 确保服务就绪并执行回调
     * @param onReady 就绪后的回调
     * @param onDenied 失败时的回调（可选）
     */
    fun ensureReady(
        onReady: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        // 引导页未完成
        if (!isIntroCompleted()) {
            onDenied?.invoke()
            return
        }

        // 非OCR模式直接成功
        if (PrefManager.workMode != WorkMode.Ocr) {
            onReady()
            return
        }

        // OCR模式已有权限
        if (ProjectionGateway.isReady()) {
            onReady()
            return
        }

        // OCR模式需要权限，但未注册launcher
        if (projectionLauncher == null) {
            Logger.e("ProjectionLauncher未注册，请先调用registerProjectionLauncher")
            onDenied?.invoke()
            return
        }

        // 设置回调并请求权限
        currentOnReady = onReady
        currentOnDenied = onDenied
        requestProjectionPermission()
    }

    /**
     * 启动核心服务
     * @param activity 启动服务的Activity上下文
     * @param forceStart 是否强制启动服务，忽略引导页检查
     * @param intent 传递给服务的Intent
     * @return 是否成功启动或准备启动服务
     */
    fun startCoreService(
        activity: BaseActivity,
        forceStart: Boolean = false,
        intent: Intent? = null
    ): Boolean {
        ensureReady(
            onReady = {
                CoreService.start(activity, intent)
            },
            onDenied = {
                Logger.w("服务启动失败：条件不满足")
            }
        )
        return true
    }

    /** 检查引导页是否完成 */
    private fun isIntroCompleted(): Boolean {
        return PrefManager.introIndex >= IntroPagerAdapter.IntroPage.APP.ordinal
    }

    /** 清理回调 */
    private fun clearCallbacks() {
        currentOnReady = null
        currentOnDenied = null
    }

    /** 请求屏幕录制权限 */
    private fun requestProjectionPermission() {
        projectionLauncher?.launch(Unit)
    }

    /** 释放资源 */
    fun release() {
        projectionLauncher = null
        clearCallbacks()
    }

    companion object {
        /**
         * 创建ServiceManager实例
         * @return 新的ServiceManager实例
         */
        fun create(): ServiceManager {
            return ServiceManager()
        }
    }
}
