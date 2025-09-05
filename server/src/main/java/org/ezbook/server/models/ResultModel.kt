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

package org.ezbook.server.models

/**
 * 统一的接口返回模型。
 *
 * @param T 业务数据的类型参数。
 * @property code 业务状态码，200 表示成功。
 * @property msg 文本消息，成功通常为 "OK"。
 * @property data 业务数据，允许为 null 以兼容无数据场景。
 */
data class ResultModel<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
) {
    companion object {
        /**
         * 构造成功结果。
         *
         * @param data 成功结果的数据，允许为 null。
         */
        fun <T> ok(data: T? = null): ResultModel<T> = ResultModel(200, "OK", data)

        /**
         * 构造错误结果。
         *
         * 使用 [Nothing?] 作为类型参数以避免无意义的数据占位，且与所有 T? 协变兼容。
         *
         * @param code 错误码。
         * @param msg 错误消息。
         */
        fun error(code: Int, msg: String): ResultModel<Any?> = ResultModel(code, msg, null)
    }
}
