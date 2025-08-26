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

/**
 * 服务管理器类
 * 负责抽象和管理应用的核心服务启动逻辑，包括权限检查、服务启动等功能
 *
 * 主要功能：
 * 1. 检查引导页完成状态
 * 2. 根据工作模式管理屏幕录制权限
 * 3. 启动核心服务
 * 4. 提供统一的服务管理接口
 */
class ServiceManager private constructor() {

    /** 屏幕录制权限启动器 */
    private var projectionLauncher: ActivityResultLauncher<Unit>? = null

    /** 权限授予成功的回调 */
    private var onServiceReady: (() -> Unit)? = null

    /** 权限被拒绝的回调 */
    private var onPermissionDenied: (() -> Unit)? = null

    /**
     * 初始化服务管理器
     * 注册屏幕录制权限请求的ActivityResultLauncher
     *
     * @param caller Activity或Fragment实例，用于注册ActivityResultLauncher
     * @param onReady 服务准备就绪时的回调函数
     * @param onDenied 权限被拒绝时的回调函数，可选
     */
    fun initialize(
        caller: ActivityResultCaller,
        onReady: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        this.onServiceReady = onReady
        this.onPermissionDenied = onDenied
        if (PrefManager.workMode != WorkMode.Ocr) {
            Logger.w("非OCR模式，无需请求屏幕录制权限")
            return
        }
        // 注册屏幕录制权限请求
        projectionLauncher = ProjectionGateway.register(
            caller = caller,
            onReady = {
                Logger.i("屏幕录制权限授予成功")
                onReady()
            },
            onDenied = {
                Logger.w("屏幕录制权限被拒绝")
                onDenied?.invoke() ?: run {
                    // 默认行为：重新请求权限
                    requestProjectionPermission()
                }
            }
        )

        Logger.i("ServiceManager初始化完成")
    }

    /**
     * 启动核心服务
     * 根据当前状态和工作模式决定启动策略
     *
     * @param activity 启动服务的Activity上下文
     * @param forceStart 是否强制启动服务，忽略引导页检查
     * @return 是否成功启动或准备启动服务
     */
    fun startCoreService(
        activity: Activity,
        forceStart: Boolean = false,
        intent: Intent? = null
    ): Boolean {
        Logger.i("开始启动核心服务，强制启动: $forceStart，工作模式: ${PrefManager.workMode}")

        // 检查引导页完成状态（除非强制启动）
        if (!forceStart && !isIntroCompleted()) {
            Logger.i("引导页未完成，跳过服务启动")
            return false
        }

        // 根据工作模式决定启动策略
        return when {
            // 非OCR模式：直接启动服务
            PrefManager.workMode != WorkMode.Ocr -> {
                Logger.i("非OCR模式，直接启动核心服务")
                CoreService.start(activity, intent)
            }

            // OCR模式且已有权限：直接启动服务
            ProjectionGateway.isReady() -> {
                Logger.i("OCR模式且屏幕录制权限已就绪，启动核心服务")
                CoreService.start(activity, intent)
            }

            // OCR模式但无权限：请求权限
            else -> {
                Logger.i("OCR模式但缺少屏幕录制权限，请求权限")
                requestProjectionPermission()
                true // 返回true表示正在处理启动流程
            }
        }
    }

    /**
     * 检查引导页是否已完成
     * @return 如果引导页已完成返回true，否则返回false
     */
    private fun isIntroCompleted(): Boolean {
        return PrefManager.introIndex >= IntroPagerAdapter.IntroPage.APP.ordinal
    }


    /**
     * 请求屏幕录制权限
     * 仅在OCR模式下需要此权限
     */
    fun requestProjectionPermission() {
        if (PrefManager.workMode != WorkMode.Ocr) {
            Logger.w("非OCR模式，无需请求屏幕录制权限")
            return
        }

        projectionLauncher?.let { launcher ->
            Logger.i("请求屏幕录制权限")
            launcher.launch(Unit)
        } ?: run {
            Logger.e("ProjectionLauncher未初始化，无法请求权限")
        }
    }

    /**
     * 获取服务状态描述
     * @return 当前服务状态的描述字符串
     */
    fun getServiceStatus(): String {
        return when {
            !isIntroCompleted() -> "引导页未完成"
            PrefManager.workMode != WorkMode.Ocr -> "服务可启动"
            ProjectionGateway.isReady() -> "OCR服务可启动"
            else -> "等待屏幕录制权限"
        }
    }


    fun isReady(): Boolean {
        return (PrefManager.workMode == WorkMode.Ocr && ProjectionGateway.isReady() || PrefManager.workMode == WorkMode.Xposed)
    }

    /**
     * 释放资源
     * 在Activity销毁时调用
     */
    fun release() {
        projectionLauncher = null
        onServiceReady = null
        onPermissionDenied = null
        Logger.i("ServiceManager资源已释放")
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
