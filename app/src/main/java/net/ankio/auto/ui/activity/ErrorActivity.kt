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

import android.os.Bundle
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import com.zackratos.ultimatebarx.ultimatebarx.addStatusBarTopPadding
import net.ankio.auto.databinding.ActivityErrorBinding
import net.ankio.auto.ui.api.BaseActivity
import net.ankio.auto.utils.AppUtils

class ErrorActivity : BaseActivity() {
    private lateinit var binding: ActivityErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent

        if (intent == null) {
            AppUtils.restart()
            return
        }

        val msg = intent.getStringExtra("msg")
        binding.errorMsg.text = msg
        binding.errorRestart.setOnClickListener {
            AppUtils.restart()
        }
        onViewCreated()
        binding.main.addStatusBarTopPadding()
        binding.main.addNavigationBarBottomPadding()
    }
}
