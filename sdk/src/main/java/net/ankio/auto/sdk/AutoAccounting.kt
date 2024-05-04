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

package net.ankio.auto.sdk

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.sdk.exception.AutoAccountingException
import net.ankio.auto.sdk.utils.RequestUtils

class AutoAccounting {
    private val PORT = 52045
    private val url = "http://127.0.0.1:$PORT/"

    private fun getSp(mContext: Context): SharedPreferences? {
        return mContext.getSharedPreferences("AutoAccountingConfig", Context.MODE_PRIVATE)
    }

    fun setToken(
        mContext: Context,
        token: String,
    ) {
        with(getSp(mContext)?.edit()) {
            this?.putString("token", token)
            this?.apply()
        }
    }

    private fun getToken(mContext: Context): String {
        val token = getSp(mContext)?.getString("token", "")
        if (token.isNullOrEmpty())
            {
                throw AutoAccountingException("token不能为空", AutoAccountingException.CODE_SERVER_AUTHORIZE)
            }
        return token
    }

    companion object {
        private lateinit var instance: AutoAccounting

        /**
         * 初始化自动记账
         * @param context 上下文
         * @param config 自动记账配置，使用JSON序列化传输
         */
        suspend fun init(
            context: Context,
            config: String,
        )  {
            instance = AutoAccounting()
            instance.setConfig(context, config)
        }

        private fun checkInit()  {
            if (!::instance.isInitialized)
                {
                    throw AutoAccountingException("AutoAccounting未初始化", AutoAccountingException.CODE_SERVER_UN_INIT)
                }
        }

        /**
         * 向自动记账请求授权后，会得到一个token，如果token为空可能未启动自动记账服务
         */
        fun setToken(
            context: Context,
            token: String?,
        ) {
            checkInit()
            if (token.isNullOrEmpty())
                {
                    throw AutoAccountingException("token不能为空", AutoAccountingException.CODE_SERVER_AUTHORIZE)
                }
            instance.setToken(context, token)
        }

        /**
         * 通过应用内广播或者hook的方式，将账本数据传递给自动记账，不支持增量传递，请全量传递。
         * 传递的数据格式为json，具体格式参考文档。
         */
        suspend fun setBooks(
            context: Context,
            books: String,
        )  {
            checkInit()
            instance.setBooks(context, books)
        }

        /**
         * 设置待报销的账单、借款等信息
         */
        suspend fun setBills(
            context: Context,
            bills: String,
            type: String,
        )  {
            checkInit()
            instance.setBills(context, bills, type)
        }

        /**
         * 获取需要同步到记账App的账单账本等信息
         */
        suspend fun getBills(context: Context): String {
            checkInit()
            return instance.getBills(context)
        }

        /**
         * @param context 上下文
         * @param config 自动记账配置，使用JSON序列化传输
         */
        suspend fun setConfig(
            context: Context,
            config: String,
        )  {
            checkInit()
            instance.setConfig(context, config)
        }

        /**
         * 设置资产数据
         * @param context 上下文
         *
         */
        suspend fun setAssets(
            context: Context,
            assets: String,
        ) {
            checkInit()
            instance.setAssets(context, assets)
        }
    }

    /**
     * 请求自动延迟尝试4次
     */
    private suspend fun request(
        path: String,
        query: HashMap<String, String> = hashMapOf(),
        data: String = "",
        context: Context,
        count: Int = 0,
    ): String =
        withContext(Dispatchers.IO) {
            runCatching {
                RequestUtils.post(
                    url = url + path,
                    query = query,
                    data = data,
                    headers = hashMapOf("Authorization" to getToken(context)),
                )
            }.getOrElse {
                if (count > 4)
                    {
                        throw it
                    } else {
                    Thread.sleep((count * 1000).toLong())
                    request(path, query, data, context, count + 1)
                }
            }
        }

    private suspend fun setAssets(
        context: Context,
        assets: String,
    ) {
        request(
            "set",
            query = hashMapOf("name" to "auto_assets"),
            data = assets,
            context = context,
        )
    }

    private suspend fun getBills(context: Context): String {
        val result =
            request(
                "get",
                query = hashMapOf("name" to "auto_bills"),
                context = context,
            )
        // 获取账单后清空原有的账单避免重复同步
        request(
            "set",
            query = hashMapOf("name" to "auto_bills"),
            data = "",
            context = context,
        )
        return result
    }

    private suspend fun setBills(
        context: Context,
        bills: String,
        type: String,
    ) {
        request(
            "set",
            query = hashMapOf("name" to "auto_bills_$type"),
            data = bills,
            context = context,
        )
    }

    private suspend fun setBooks(
        context: Context,
        books: String,
    ) {
        request(
            "set",
            query = hashMapOf("name" to "auto_books"),
            data = books,
            context = context,
        )
    }

    private suspend fun setConfig(
        context: Context,
        config: String,
    ) {
        request(
            "set",
            query = hashMapOf("name" to "bookAppConfig"),
            data = config,
            context = context,
        )
    }
}
