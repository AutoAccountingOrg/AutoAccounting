/*
 * Copyright (C) 2023 ankio(ankio@ankio.net)
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

package org.ezbook.server.tools

import android.content.Context
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.log.ServerLog

/**
 * 账单管理器
 *
 * 职责：协调转账识别、去重识别和账单管理三个模块
 *
 * 处理流程：
 * 1. 优先检查转账识别（如果启用）
 * 2. 然后检查重复账单（如果启用）
 * 3. 提供备注生成功能
 */
object BillManager {

    /**
     * 账单去重和转账识别的主入口
     *
     * 处理流程：
     * 1. 优先检查转账识别（如果启用）
     * 2. 然后检查重复账单（如果启用）
     *
     * @param billInfoModel 当前账单
     * @return 返回匹配到的父账单，如果没有匹配则返回null
     *         注意：返回的父账单分类信息已被清空，需要调用方重新分类
     */
    suspend fun groupBillInfo(billInfoModel: BillInfoModel): BillInfoModel? {
        ServerLog.d("账单处理：开始，bill=${billInfoModel.id}, money=${billInfoModel.money}, type=${billInfoModel.type}")

        var current = billInfoModel
        // 优先检查转账识别
        val transferBill = TransferRecognizer.recognize(current)
        if (transferBill != null) {
            ServerLog.d("账单处理：识别到转账账单，parentId=${transferBill.id}, currentId=${current.id}")
            TransferRecognizer.process(current, transferBill)
            current = transferBill
            return current
        }

        // 检查重复账单
        val duplicateBill = DuplicateDetector.detect(current)
        if (duplicateBill != null) {
            ServerLog.d("账单处理：识别到重复账单，parentId=${duplicateBill.id}, currentId=${current.id}")
            return duplicateBill
        }

        ServerLog.d("账单处理：未匹配到重复或转账账单")
        return null
    }

    /**
     * 获取备注
     * 
     * @param billInfoModel 账单信息
     * @param context 上下文
     * @return 格式化后的备注字符串
     */
    suspend fun getRemark(billInfoModel: BillInfoModel, context: Context): String {
        return BillMerger.getRemark(billInfoModel, context)
    }
}
