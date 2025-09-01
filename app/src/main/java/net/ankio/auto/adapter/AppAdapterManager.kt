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

package net.ankio.auto.adapter

import net.ankio.auto.App
import net.ankio.auto.constant.WorkMode
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.PrefManager
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.model.BillInfoModel

/**
 * 记账应用适配器管理器。
 *
 * 职责：
 * - 根据当前工作模式（OCR / Xposed）提供便捷判断方法。
 * - 提供受支持的记账应用适配器列表。
 * - 根据用户在偏好设置中选择的记账应用，返回对应的适配器实例；若未匹配到则回退为仅记录到本应用的 `AutoAdapter`。
 */
object AppAdapterManager {

    /**
     * 是否处于 OCR 工作模式。
     *
     * 返回 true 表示当前通过截图/OCR 识别进行记账；
     * 返回 false 表示不处于 OCR 模式（可能为 Xposed 或其他模式）。
     */
    fun ocrMode(): Boolean = PrefManager.workMode === WorkMode.Ocr

    /**
     * 是否处于 Xposed 工作模式。
     *
     * 返回 true 表示通过 Xposed Hook 的方式与目标记账应用交互；
     * 返回 false 表示不处于 Xposed 模式。
     */
    fun xposedMode(): Boolean = PrefManager.workMode === WorkMode.Xposed


    /**
     * 返回受支持的记账应用适配器清单。
     *
     * 如需新增适配器，请在此列表中追加实例；
     * 列表顺序一般不影响业务选择逻辑，但可能影响到 UI 展示时的默认排序。
     */
    fun adapterList(): List<IAppAdapter> {
        return listOf(
            AutoAdapter(), // 仅自动记账（不向第三方应用同步）
            QianJiAdapter(), // 钱迹
            YiYuAdapter(), // 一羽记账
            YiMuAdapter(), // 一木记账
            XiaoXinAdapter(), // 小星记账
        )
    }

    /**
     * 获取当前选定的记账应用适配器实例。
     *
     * 匹配规则：根据偏好设置中的包名（`PrefManager.bookApp`）从 `adapterList` 中查找；
     * 若未匹配到任何适配器，则回退为 `AutoAdapter`，即仅在本应用内完成记账记录。
     */
    fun adapter(): IAppAdapter {
        return adapterList().firstOrNull { it.pkg == PrefManager.bookApp } ?: AutoAdapter()
    }

    fun markSynced(billInfoModel: BillInfoModel) {
        billInfoModel.state = BillState.Synced
        App.launch {
            BillAPI.put(billInfoModel)
        }

    }
}