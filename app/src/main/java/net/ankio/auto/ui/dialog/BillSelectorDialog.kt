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
import net.ankio.auto.databinding.DialogBillSelectBinding
import net.ankio.auto.http.api.BookBillAPI
import net.ankio.auto.storage.Constants
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.adapter.BillSelectorAdapter
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.WrapContentLinearLayoutManager
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.Setting
import io.github.oshai.kotlinlogging.KotlinLogging

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
    private var billType: String = Setting.HASH_BILL // 默认值
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

    /**
     * 设置账单类型
     * @param type 账单类型，如 Setting.HASH_BAOXIAO_BILL 或 Setting.HASH_BILL
     * @return 当前对话框实例，支持链式调用
     */
    fun setBillType(type: String) = apply {
        this.billType = type
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
        val multipleSelect = billType == Setting.HASH_BAOXIAO_BILL
        adapter = BillSelectorAdapter(selectedBills, multipleSelect)
        recyclerView.adapter = adapter

        logger.debug { "RecyclerView设置完成，多选模式: $multipleSelect" }
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
            val proactively = PrefManager.featureLeading
            
            if (proactively) {
                syncDataIfNeeded()
            }

            loadDataWithTimeout(proactively)
        }
    }

    /**
     * 同步数据（如果需要）
     */
    private suspend fun syncDataIfNeeded() = withContext(Dispatchers.IO) {
        try {
            //TODO 每个Adapter应该自己提供记账数据
            // 同步基础数据
            //BookAppUtils.syncData()

            // 清空现有数据
            BookBillAPI.put(arrayListOf(), "", billType)

            // 检查同步间隔
            val lastSyncTime = PrefManager.lastSyncTime
            val now = System.currentTimeMillis()

            if (now - lastSyncTime > Constants.SYNC_INTERVAL) {
                PrefManager.lastSyncTime = now

                when (billType) {
                    Setting.HASH_BAOXIAO_BILL -> {}//BookAppUtils.syncReimburseBill()
                    Setting.HASH_BILL -> {}//BookAppUtils.syncRecentExpenseBill()
                }

                logger.debug { "同步完成，账单类型: $billType" }
            }
        } catch (e: Exception) {
            logger.error(e) { "同步数据失败" }
        }
    }

    /**
     * 带超时的数据加载
     */
    private suspend fun loadDataWithTimeout(proactively: Boolean) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val timeout = 10000 // 10秒超时

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                val bills = BookBillAPI.list(billType)
                logger.debug { "获取到账单数量: ${bills.size}" }

                if (bills.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.statusPage.showContent()
                        adapter.updateItems(bills)
                    }
                    return@withContext
                }

                // 如果不是主动模式且没有数据，直接显示空状态
                if (!proactively) {
                    break
                }

                // 等待500ms后重试
                delay(500)

            } catch (e: Exception) {
                logger.error(e) { "加载账单数据失败" }
                break
            }
        }

        // 显示空状态
        withContext(Dispatchers.Main) {
            binding.statusPage.showEmpty()
            logger.debug { "账单数据为空或加载超时" }
        }
    }


}