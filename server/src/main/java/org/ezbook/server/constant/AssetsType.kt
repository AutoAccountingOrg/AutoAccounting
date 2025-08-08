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
    NORMAL, // 普通资产，例如储蓄卡、借记卡、支付宝、微信、等具有固定余额的账户

    /* VIRTUAL, // 虚拟资产，例如积分、优惠券、话费、公交卡余额等需要预先付费（一般不可退回）的账户
     FINANCIAL, // 理财资产，例如股票、基金、余额宝、余利宝等，存在动态波动（增减）的资产
      // 信用资产，例如信用卡、蚂蚁花呗，属于小额借贷，有额度限制的账户*/
    CREDIT,
    BORROWER, // 欠款人，例如借款给别人的钱
    CREDITOR, // 债主，例如别人借钱给自己
}