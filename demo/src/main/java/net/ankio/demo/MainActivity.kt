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

package net.ankio.demo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import net.ankio.auto.sdk.AutoAccounting
import net.ankio.auto.sdk.exception.AutoAccountingException
import net.ankio.auto.sdk.utils.Logger
import net.ankio.common.constant.BillType

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //注册授权请求

        val requestAuthorization = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data: Intent? = result.data
                val resultData = data?.getStringExtra("token")
                Toast.makeText(this,"接收到的数据: $resultData",Toast.LENGTH_SHORT).show()
                //调用自动记账存储
                try {
                    AutoAccounting.setToken(this, resultData)

                    //授权成功后可以一次性将所有数据先同步给自动记账

                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this,"数据为空，可能是因为自动记账服务未启动",Toast.LENGTH_SHORT).show()
                }
                //授权成功尝试重启
                tryStartAutoAccounting()
            }else{
                Toast.makeText(this,"授权失败或者用户拒绝授权",Toast.LENGTH_SHORT).show()
            }
        }
        //尝试启动自动记账服务
        tryStartAutoAccounting()
        //请求授权
        findViewById<Button>(R.id.authorization).setOnClickListener {
            //设置Action
            val intent = Intent("net.ankio.auto.ACTION_REQUEST_AUTHORIZATION")
            //设置包名，用于自动记账对目标app进行检查
            intent.putExtra("packageName", packageName)
            try {
                //启动授权请求
                requestAuthorization.launch(intent)
            }catch (e: ActivityNotFoundException){
                //没有自动记账，需要引导用户下载自动记账App
                e.printStackTrace()
                Toast.makeText(this,"未找到自动记账，请从xxxx下载自动记账App",Toast.LENGTH_SHORT).show()
            }
        }
        //设置账本数据
        findViewById<Button>(R.id.syncBook).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val books =  Gson().toJson(MockUtils.generateRandomBooks(4))
                    Logger.i("books:$books")
                    AutoAccounting.setBooks(this@MainActivity,books)
                    Toast.makeText(this@MainActivity,"设置成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"设置失败",Toast.LENGTH_SHORT).show()
                }
            }

        }
        //设置资产数据
        findViewById<Button>(R.id.syncAssets).setOnClickListener {
            lifecycleScope.launch {
                try {
                    AutoAccounting.setAssets(this@MainActivity,Gson().toJson(MockUtils.generateAssets(30)))
                    Toast.makeText(this@MainActivity,"设置成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"设置失败",Toast.LENGTH_SHORT).show()
                }
            }

        }
        //设置待报销账单
        findViewById<Button>(R.id.syncReimbursement).setOnClickListener {
            lifecycleScope.launch {
                try {
                    AutoAccounting.setBills(this@MainActivity,Gson().toJson(MockUtils.generateRandomBills(30)),
                        BillType.ExpendReimbursement //支出时标记为报销的账单
                    )
                    Toast.makeText(this@MainActivity,"设置成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"设置失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
        //同步债务账单给自动记账（借钱给别人）

        findViewById<Button>(R.id.syncDebtExpend).setOnClickListener {
            lifecycleScope.launch {
                try {
                    AutoAccounting.setBills(this@MainActivity,Gson().toJson(MockUtils.generateRandomBills(10)),
                        BillType.ExpendLending //借钱给别人
                    )
                    Toast.makeText(this@MainActivity,"设置成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"设置失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
        //同步债务账单给自动记账（借别人钱）
        findViewById<Button>(R.id.syncDebtIncome).setOnClickListener {
            lifecycleScope.launch {
                try {
                    AutoAccounting.setBills(this@MainActivity,Gson().toJson(MockUtils.generateRandomBills(10)),
                        BillType.IncomeLending //借别人钱
                    )
                    Toast.makeText(this@MainActivity,"设置成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"设置失败",Toast.LENGTH_SHORT).show()
                }
            }
        }
        //从自动记账获取需要同步的账单
        findViewById<Button>(R.id.syncBills).setOnClickListener {
            //这一步放在onresume里面
            lifecycleScope.launch {
                try {
                    val bills = AutoAccounting.getBills(this@MainActivity)
                    Logger.i("bills:$bills")
                    Toast.makeText(this@MainActivity,"获取成功",Toast.LENGTH_SHORT).show()
                }catch (e:AutoAccountingException){
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity,"获取失败",Toast.LENGTH_SHORT).show()
                }
            }
        }

    }
    private fun tryStartAutoAccounting() {
        lifecycleScope.launch {
            try {
                //
                AutoAccounting.init(
                    this@MainActivity,
                    Gson().toJson(MockUtils.generateRandomConfig()),//自动记账的demo使用的是Gson库来序列化对象，你也可以使用其他的库来序列化，只要保证传输给自动记账的数据是json的即可。
                )
                Toast.makeText(this@MainActivity,"自动记账服务已连接",Toast.LENGTH_SHORT).show()
            } catch (e: AutoAccountingException) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity,"自动记账服务未启动或自动记账未授权服务",Toast.LENGTH_SHORT).show()
            }
        }
    }

    //随机账本数据




    private fun randImage(){

    }





}