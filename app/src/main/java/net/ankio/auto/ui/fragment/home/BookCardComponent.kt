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

package net.ankio.auto.ui.fragment.home

import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.google.android.material.elevation.SurfaceColors
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.CardBookBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.components.IconTileView
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.utils.PrefManager

class BookCardComponent(binding: CardBookBinding, private val lifecycle: Lifecycle) :
    BaseComponent<CardBookBinding>(binding, lifecycle) {


    private lateinit var onRedirect: (id: Int) -> Unit


    fun setOnRedirect(it: (id: Int) -> Unit) {
        onRedirect = it
    }

    override fun init() {
        super.init()
        binding.btnSwitch.setOnClickListener {
            // TODO 选择记账软件
        }

        binding.btnRefresh.setOnClickListener {
            val adapter = AppAdapterManager.adapter()
            adapter.syncAssets()
        }

        initActionGrid(actionItems())

        binding.root.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
    }

    override fun resume() {
        super.resume()
        val adapter = AppAdapterManager.adapter()
        binding.btnRefresh.visibility = if (adapter.supportSyncAssets()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.tvAppName.text = adapter.name
        binding.tvPackageName.text = adapter.pkg
        Glide.with(context).load(adapter.icon)
            .error(R.mipmap.ic_launcher)
            .fallback(R.mipmap.ic_launcher)
            .into(binding.ivAppIcon)
    }


    /**
     * 生成首页网格所需的 ActionTile 列表
     */
    private fun actionItems(): List<ActionTile> {
        val list = ArrayList<ActionTile>()

        // ——— 账本管理（多账本功能开启时才出现，可在其中选择默认账本） ———
        if (PrefManager.featureMultiBook) {
            list.add(
                ActionTile(
                    icon = R.drawable.ic_book,
                    label = R.string.title_books,
                    circleColor = R.color.tile_bg_books,
                    onClick = { openBookManager() }
                )
            )
        }

        // ——— 分类管理 ———
        list.add(
            ActionTile(
                icon = R.drawable.ic_category,
                label = R.string.title_category,
                circleColor = R.color.tile_bg_category,
                onClick = { openCategoryManager() }
            )
        )

        // ——— 分类映射 ———
        list.add(
            ActionTile(
                icon = R.drawable.ic_swap_horiz,
                label = R.string.title_category_mapping,
                circleColor = R.color.tile_bg_category_map,
                onClick = { openCategoryMapping() }
            )
        )

        // ——— 资产相关（需开启资产管理功能） ———
        if (PrefManager.featureAssetManage) {
            // 资产管理（普通资产、债务）
            list.add(
                ActionTile(
                    icon = R.drawable.ic_account_balance_wallet,
                    label = R.string.title_assets,
                    circleColor = R.color.tile_bg_assets,
                    onClick = { openAssetManager() }
                )
            )

            // 资产映射
            list.add(
                ActionTile(
                    icon = R.drawable.ic_compare_arrows,
                    label = R.string.title_assets_map,
                    circleColor = R.color.tile_bg_assets_map,
                    onClick = { openAssetMapping() }
                )
            )
        }

        // ——— 标签管理（仅当标签功能启用） ———
        if (PrefManager.featureTag) {
            list.add(
                ActionTile(
                    icon = R.drawable.ic_label,
                    label = R.string.title_tag,
                    circleColor = R.color.tile_bg_tag,
                    onClick = { openTagManager() }
                )
            )
        }

        // 应用监控白名单
        list.add(
            ActionTile(
                icon = R.drawable.home_app_book,
                label = R.string.title_app_whitelist,
                circleColor = R.color.tile_bg_app_whitelist,
                onClick = { openAppWhitelist() }
            )
        )

        // 账单数据过滤关键词
        list.add(
            ActionTile(
                icon = R.drawable.data_filter,
                label = R.string.title_data_filter,
                circleColor = R.color.tile_bg_data_filter,
                onClick = { openDataFilter() }
            )
        )

        return list
    }

    /* -------------------------------------------------------
     * 数据模型保持不变
     * ----------------------------------------------------- */
    data class ActionTile(
        @DrawableRes val icon: Int,
        @StringRes val label: Int,
        @ColorRes val circleColor: Int,
        val onClick: () -> Unit
    )

    /* -------------------------------------------------------
     * 占位回调：请替换成真正实现
     * ----------------------------------------------------- */
    private fun openBookManager() {
        onRedirect.invoke(R.id.action_homeFragment_to_bookFragment)
    }

    private fun openCategoryManager() {
        onRedirect.invoke(R.id.action_homeFragment_to_categoryFragment)
    }

    private fun openCategoryMapping() {
        onRedirect.invoke(R.id.action_homeFragment_to_categoryMapFragment)
    }

    private fun openAssetManager() { /* TODO 跳转资产管理页面（根据自动记账定义的资产拆分）*/
    }

    private fun openAssetMapping() { /* TODO 跳转资产映射 */
    }

    private fun openTagManager() { /* TODO 跳转标签管理 */
    }

    private fun openAppWhitelist() { /* TODO 跳转应用监控白名单管理页面 */
    }

    private fun openDataFilter() { /* TODO 跳转账单数据过滤关键词管理页面 */
    }


    /** 在 Activity/Fragment 初始化时调用 */
    private fun initActionGrid(actions: List<ActionTile>) {
        val grid = binding.gridActions                    // ← XML 里的 id
        grid.removeAllViews()

        actions.forEachIndexed { index, tile ->
            val view = IconTileView(context).apply {
                setIcon(tile.icon)
                setLabel(context.getString(tile.label))
                setCircleColor(context.getColor(tile.circleColor))
                setOnClickListener { tile.onClick() }
            }

            // 每列平均宽：width=0 + columnWeight=1
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 4, 1f)   // 4 列
            }
            grid.addView(view, lp)
        }
    }
}