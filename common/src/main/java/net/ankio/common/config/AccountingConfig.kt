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

package net.ankio.common.config

data class AccountingConfig(
    val assetManagement: Boolean = false,//是否开启资产管理
    val multiCurrency: Boolean = false,//是否开启多币种
    val reimbursement: Boolean = false,//是否开启报销
    val lending: Boolean = false,//是否开启债务功能
    val multiBooks: Boolean = false,//是否开启多账本
    val fee: Boolean = false,//是否开启手续费
)