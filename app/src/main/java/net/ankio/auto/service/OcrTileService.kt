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

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.activity.FloatingWindowTriggerActivity
import org.ezbook.server.intent.OCRIntent

/**
 * OCR快速设置磁贴服务
 * 用户可以通过下拉通知栏的快速设置磁贴来启动OCR识别
 */
class OcrTileService : TileService() {

    /**
     * 磁贴开始监听时调用
     * 用于更新磁贴状态
     */
    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    /**
     * 磁贴停止监听时调用
     */
    override fun onStopListening() {
        super.onStopListening()
    }

    /**
     * 更新磁贴状态
     * 只有在OCR模式下才启用磁贴，其他模式下禁用
     */
    private fun updateTileState() {
        val tile = qsTile ?: return

        if (AppAdapterManager.ocrMode()) {
            // OCR模式：启用磁贴
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.ocr_tile_title)
            Logger.d("OCR磁贴已启用")
        } else {
            // 非OCR模式：禁用磁贴
            tile.state = Tile.STATE_UNAVAILABLE
            tile.label = getString(R.string.ocr_tile_title)
            Logger.d("OCR磁贴已禁用（非OCR模式）")
        }

        tile.updateTile()
    }

    /**
     * 磁贴被点击时调用
     */
    override fun onClick() {
        super.onClick()

        Logger.d("OCR磁贴被点击")

        // 检查是否为OCR模式
        if (!AppAdapterManager.ocrMode()) {
            Logger.w("当前不是OCR模式，忽略磁贴点击")
            return
        }

        try {
            // 创建启动FloatingWindowTriggerActivity的Intent
            val intent = OCRIntent().toIntent()

            // 根据Android版本收起面板
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                startActivityAndCollapse(pendingIntent)
            } else {
                // Android N-13 使用 Intent
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }


        } catch (e: Exception) {
            Logger.e("通过磁贴启动OCR识别失败: ${e.message}")
        }
    }

    /**
     * 服务销毁时调用
     * 清理资源，防止内存泄露
     */
    override fun onDestroy() {
        Logger.d("OCR磁贴服务销毁，清理资源")
        super.onDestroy()
    }
}
