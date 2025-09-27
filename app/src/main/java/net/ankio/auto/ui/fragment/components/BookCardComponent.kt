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

package net.ankio.auto.ui.fragment.components

import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.elevation.SurfaceColors
import com.google.gson.Gson
import net.ankio.auto.R
import net.ankio.auto.adapter.AppAdapterManager
import net.ankio.auto.databinding.CardBookBinding
import net.ankio.auto.ui.api.BaseComponent
import net.ankio.auto.ui.api.BaseSheetDialog
import net.ankio.auto.ui.components.IconTileView
import net.ankio.auto.ui.dialog.AppDialog
import net.ankio.auto.ui.dialog.BookSelectorDialog
import net.ankio.auto.ui.fragment.CategoryFragment
import net.ankio.auto.ui.theme.DynamicColors
import net.ankio.auto.ui.utils.AppUtils
import net.ankio.auto.ui.utils.PaletteManager
import net.ankio.auto.ui.utils.load
import net.ankio.auto.utils.PrefManager

class BookCardComponent(binding: CardBookBinding) :
    BaseComponent<CardBookBinding>(binding) {

    /** 页面跳转回调 */
    private var onRedirect: ((id: Int, android.os.Bundle?) -> Unit)? = null


    override fun onComponentDestroy() {
        onRedirect = null
        super.onComponentDestroy()
    }

    fun setOnRedirect(callback: (id: Int, android.os.Bundle?) -> Unit) {
        onRedirect = callback
    }

    override fun onComponentCreate() {
        super.onComponentCreate()
        binding.app.setOnClickListener {
            BaseSheetDialog.create<AppDialog>(context)
                .setOnClose {
                    onComponentResume()
                }
                .show()
        }

        binding.btnRefresh.setOnClickListener {
            val adapter = AppAdapterManager.adapter()
            adapter.syncAssets()
        }

        initActionGrid(actionItems())

        binding.cardHeader.setCardBackgroundColor(DynamicColors.SurfaceColor1)
        binding.cardActions.setCardBackgroundColor(DynamicColors.SurfaceColor1)
        //  binding.root.setCardBackgroundColor(DynamicColors.SurfaceColor1)
    }

    override fun onComponentResume() {
        super.onComponentResume()
        val adapter = AppAdapterManager.adapter()
        binding.btnRefresh.visibility = if (adapter.supportSyncAssets()) {
            View.VISIBLE
        } else {
            View.GONE
        }
        binding.tvAppName.text = adapter.name
        binding.tvPackageName.text = adapter.pkg
        val appInfo = AppUtils.get(adapter.pkg)
        binding.ivAppIcon.setImageDrawable(appInfo?.icon)
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
                    onClick = { openBookManager() }
                )
            )
        }

        // ——— 分类管理 ———
        list.add(
            ActionTile(
                icon = R.drawable.ic_category,
                label = R.string.title_category,
                onClick = { openCategoryManager() }
            )
        )

        // ——— 分类映射 ———
        list.add(
            ActionTile(
                icon = R.drawable.ic_swap_horiz,
                label = R.string.title_category_mapping,
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
                    onClick = { openAssetManager() }
                )
            )

            // 资产映射
            list.add(
                ActionTile(
                    icon = R.drawable.ic_compare_arrows,
                    label = R.string.title_assets_map,
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
                    onClick = { openTagManager() }
                )
            )
        }

        // 应用监控白名单
        list.add(
            ActionTile(
                icon = R.drawable.home_app_book,
                label = R.string.title_app_whitelist,
                onClick = { openAppWhitelist() }
            )
        )

        // 账单数据过滤关键词
        list.add(
            ActionTile(
                icon = R.drawable.data_filter,
                label = R.string.title_data_filter,
                onClick = { openDataFilter() }
            )
        )

        return list
    }

    /* -------------------------------------------------------
     * 数据模型 - 基于 label 动态分配颜色
     * ----------------------------------------------------- */
    data class ActionTile(
        @DrawableRes val icon: Int,
        @StringRes val label: Int,
        val onClick: () -> Unit
    )

    /* -------------------------------------------------------
     * 占位回调：请替换成真正实现
     * ----------------------------------------------------- */
    private fun openBookManager() {
        // 使用目的地 ID 导航，减少对当前目的地 action 的依赖
        onRedirect?.invoke(R.id.bookFragment, null)
    }

    private fun openCategoryManager() {
        BaseSheetDialog.create<BookSelectorDialog>(context)
            .setShowSelect(false)
            .setCallback { bookModel, _ ->
                val bundle = android.os.Bundle().apply {
                    putString(CategoryFragment.ARG_BOOK_MODEL, Gson().toJson(bookModel))
                }
                // 使用目的地 ID 导航，避免当前目的地不为 HomeFragment 时找不到 action
                onRedirect?.invoke(R.id.categoryFragment, bundle)
            }
            .show()
    }

    private fun openCategoryMapping() {
        // 使用目的地 ID 导航
        onRedirect?.invoke(R.id.categoryMapFragment, null)
    }

    private fun openAssetManager() {
        // 使用目的地 ID 导航
        onRedirect?.invoke(R.id.assetFragment, null)
    }

    private fun openAssetMapping() {
        // 这里使用目的地 ID 而非 action ID，避免当当前目的地被识别为 NavGraph
        // （而不是具体 Fragment）时，抛出 “action/destination ... cannot be found from the current destination NavGraph” 的异常。
        // 直接导航到目的地可在整个图内解析节点，减少对当前目的地 action 的耦合。
        onRedirect?.invoke(R.id.assetMapFragment, null)
    }

    private fun openTagManager() {
        // 使用目的地 ID 导航
        onRedirect?.invoke(R.id.tagFragment, null)
    }

    private fun openAppWhitelist() {
        // 使用目的地 ID 导航
        onRedirect?.invoke(R.id.appWhiteListFragment, null)
    }

    private fun openDataFilter() {
        // 使用目的地 ID 导航
        onRedirect?.invoke(R.id.dataFilterFragment, null)
    }


    /** 在 Activity/Fragment 初始化时调用 */
    private fun initActionGrid(actions: List<ActionTile>) {
        val grid = binding.gridActions                    // ← XML 里的 id
        grid.removeAllViews()

        actions.forEachIndexed { index, tile ->
            val labelText = context.getString(tile.label)
            val colors = PaletteManager.getColorsByLabel(context, labelText)
            val view = IconTileView(context).apply {
                setIcon(tile.icon)
                setLabel(labelText)
                setColors(colors)  // 直接使用双色方案
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