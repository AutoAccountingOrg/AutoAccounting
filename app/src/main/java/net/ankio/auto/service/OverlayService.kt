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

class OverlayService : ICoreService() {

    private lateinit var billWindowManager: BillWindowManager


    fun service() = coreService

    override fun onCreate(coreService: CoreService) {
        super.onCreate(coreService)
        billWindowManager = BillWindowManager(this)

    }

    override fun onDestroy() {
        billWindowManager.destroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) {
        val floatIntent = BillInfoIntent.parse(intent!!) ?: return
        val parent = floatIntent.parent
        if (parent != null) {
            if (PrefManager.showDuplicatedPopup) {
                //说明是重复账单
                ToastUtils.info(coreService.getString(R.string.repeat_bill))
            }
            billWindowManager.updateCurrentBill(parent)
            Logger.d("Repeat Bill, Parent: $parent")
        } else {
            billWindowManager.addBill(floatIntent.billInfoModel)
        }
    }

    companion object : IService {
        override fun hasPermission(): Boolean {
            return Settings.canDrawOverlays(autoApp)
        }

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