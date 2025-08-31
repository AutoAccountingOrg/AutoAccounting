/*
 * Copyright (C) 2025 ankio
 * Licensed under the Apache License, Version 3.0
 */
package net.ankio.auto.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import net.ankio.auto.R
import net.ankio.auto.ui.components.IconView
import net.ankio.auto.ui.utils.setAssetIcon
import org.ezbook.server.constant.Currency

/**
 * 货币下拉适配器：在下拉列表与已选中视图中同时显示图标与名称
 */
class CurrencyDropdownAdapter(
    context: Context,
    private val items: List<Currency>
) : ArrayAdapter<Currency>(context, 0, items) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return bindView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return bindView(position, convertView, parent)
    }

    private fun bindView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View =
            convertView ?: inflater.inflate(R.layout.item_currency_dropdown, parent, false)
        val iconView: IconView = view.findViewById(R.id.currencyItem)

        val item = items[position]
        // 显示货币符号 + 名称，与选中后的显示格式保持一致
        val displayText = item.name(context)
        iconView.setText(displayText)
        iconView.setTint(false)
        iconView.imageView().setAssetIcon(item.iconUrl())
        return view
    }
}


