/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
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

package net.ankio.auto.ui.activity

import android.content.Intent
import android.os.Bundle
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import net.ankio.auto.R
import net.ankio.auto.databinding.ActivityAuthBinding
import net.ankio.auto.utils.AppUtils
import net.ankio.auto.utils.AutoAccountingServiceUtils
import net.ankio.auto.utils.SpUtils


class AuthActivity :  BaseActivity() {
    private lateinit var binding: ActivityAuthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent

        if (intent == null) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val packageName = intent.getStringExtra("packageName")

        if (packageName.isNullOrEmpty()){
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val appInfo = AppUtils.getAppInfoFromPackageName(packageName, this@AuthActivity)

        if(appInfo == null){
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        binding.authInfo.text = getString(R.string.auth_msg, appInfo.name)

        binding.sure.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("token", AutoAccountingServiceUtils.getToken())
            SpUtils.putString("bookApp",packageName)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        binding.cancel.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        onViewCreated()
        binding.main.addStatusBarTopPadding()
        binding.main.addNavigationBarBottomPadding()
    }
}