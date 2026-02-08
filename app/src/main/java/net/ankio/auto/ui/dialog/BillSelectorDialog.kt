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

package net.ankio.auto.ui.dialog

import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.ankio.auto.databinding.DialogBillSelectBinding
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.storage.Constants
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BillSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.utils.PrefManager
import net.ankio.auto.adapter.AppAdapterManager
import org.ezbook.server.constant.Setting
import org.ezbook.server.constant.BillAction
import org.ezbook.server.db.model.BookBillModel

/**
 * 账单选择对话框
 *
 * 设计原则：
 * 1. 单一职责：只负责账单选择逻辑
 * 2. 简洁实现：消除复杂状态管理，统一处理流程
 * 3. 类型安全：使用强类型参数替代魔法值
 * 4. 良好错误处理：超时机制和用户反馈
 *
 * 用途：
 * - 报销账单选择（多选模式）
 * - 退款账单选择（单选模式）
 *
 * 使用方式：
 * ```kotlin
 * BillSelectorDialog.create(activity)
 *     .setSelectedBills(selectedList)
 *     .setBillType(Setting.HASH_BAOXIAO_BILL)
 *     .setCallback { /* 处理选择结果 */ }
 *     .show()
 * ```
 */
class BillSelectorDialog internal constructor(
    context: android.content.Context
) : BaseSheetDialog<DialogBillSelectBinding>(context) {

    private var selectedBills: MutableList<String> = mutableListOf()
    private var billType: BillAction = BillAction.SYNC_REIMBURSE_BILL
    private var callback: (() -> Unit)? = null

    private lateinit var adapter: BillSelectorAdapter

    /**
     * 设置已选择的账单列表
     * @param bills 已选择的账单ID列表
     * @return 当前对话框实例，支持链式调用
     */
    fun setSelectedBills(bills: MutableList<String>) = apply {
        this.selectedBills = bills
    }

    var bookName: String = ""
    /**
     * 设置账单类型
     * @param type 账单类型，如 Setting.HASH_BAOXIAO_BILL 或 Setting.HASH_BILL
     * @return 当前对话框实例，支持链式调用
     */
    fun setBillType(type: BillAction, bookName: String) = apply {
        this.billType = type
        this.bookName = bookName
    }

    /**
     * 设置选择完成回调
     * @param callback 选择完成后的回调函数
     * @return 当前对话框实例，支持链式调用
     */
    fun setCallback(callback: () -> Unit) = apply {
        this.callback = callback
    }

    override fun onViewCreated(view: View?) {
        super.onViewCreated(view)
        setupRecyclerView()
        setupConfirmButton()
        loadBillData()
    }

    /**
     * 设置列表视图
     */
    private fun setupRecyclerView() {
        val recyclerView = binding.statusPage.contentView!!
        recyclerView.layoutManager = WrapContentLinearLayoutManager(ctx)

        // 判断是否支持多选（报销账单支持多选）
        val multipleSelect = billType == BillAction.SYNC_REIMBURSE_BILL
        adapter = BillSelectorAdapter(selectedBills, multipleSelect)
        recyclerView.adapter = adapter

        Logger.d("RecyclerView设置完成，多选模式: $multipleSelect")
    }

    /**
     * 设置确认按钮
     */
    private fun setupConfirmButton() {
        binding.confirmButton.setOnClickListener {
            callback?.invoke()
            dismiss()
        }
    }

    /**
     * 加载账单数据
     */
    private fun loadBillData() {
        binding.statusPage.showLoading()

        launch {
            loadDataWithTimeout()

        }
    }

    /**
     * 带超时的数据加载（withTimeout 版本）
     * - 主动模式：在超时时间内轮询，直到拿到数据或超时
     * - 非主动模式：立即拉取一次，若为空直接返回
     */
    private suspend fun loadDataWithTimeout() = withContext(Dispatchers.IO) {
        val timeoutMs = 30_000L
        BookBillAPI.put(arrayListOf(), "", billType.name)
        withContext(Dispatchers.Main) {
            AppAdapterManager.adapter().syncWaitBills(billType, bookName)
        }
        // 按需求：进入页面先发起请求并显示 loading，2 秒后再开始检查数据
        delay(2_000)
        val bills = withTimeoutOrNull(timeoutMs) {
            val result: List<BookBillModel>
            while (true) {
                val list =
                    runCatching { BookBillAPI.list(if (billType == BillAction.SYNC_RECENT_EXPENSE_BILL) Setting.HASH_BILL else Setting.HASH_BAOXIAO_BILL) }
                        .onFailure { Logger.e("加载账单数据失败", it) }
                        .getOrElse { emptyList() }

                Logger.d("获取到账单数量: ${list.size}")

                if (list.isNotEmpty()) {
                    result = list
                    break
                }

                delay(500)
            }
            result
        } ?: emptyList()

        // 清理外部传入但已不存在的报销ID/账单ID
        // 规则：
        // - 以服务端返回的账单 remoteId 作为唯一判定依据
        // - 对于空列表，视为没有可选账单，清空已选列表
        run {
            val before = selectedBills.size
            val validIds: Set<String> = bills.map { it.remoteId }.toSet()
            selectedBills.removeAll { it !in validIds }
            val removedCount = before - selectedBills.size
            if (removedCount > 0) {
                Logger.d("已移除不存在的账单ID: $removedCount 个")
            }
        }

        if (bills.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                binding.statusPage.showContent()
                adapter.updateItems(bills)
            }
        } else {
            withContext(Dispatchers.Main) {
                binding.statusPage.showEmpty()
                Logger.d("账单数据为空或加载超时")
            }
        }
    }


}