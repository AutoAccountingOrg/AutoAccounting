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

package org.ezbook.server.constant

enum class AssetsType {
    NORMAL, // 普通，实实在在属于自己的资产，例如银行卡、支付宝、微信
    FINANCIAL, // 金融(理财）资产，例如股票、基金，属于会存在动态波动（增减）的资产
    CREDIT, // 信用资产，例如信用卡、蚂蚁花呗
    BORROWE, // 借款人，例如借款给别人的钱
    CREDITOR, // 债权人，例如别人借钱给自己
}