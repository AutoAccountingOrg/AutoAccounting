package net.ankio.auto.ui.adapter

import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterAssetsBinding
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.ResourceUtils
import org.ezbook.server.constant.AssetsType
import org.ezbook.server.db.model.AssetsModel

class AssetsSelectorAdapter(
    private val list: MutableList<AssetsModel>,
    private val callback: (AssetsModel) -> Unit
) : BaseAdapter<AdapterAssetsBinding, AssetsModel>(
    AdapterAssetsBinding::class.java, list
) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAssetsBinding, AssetsModel>) {
        holder.binding.root.setOnClickListener {
            callback(holder.item!!)
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAssetsBinding, AssetsModel>,
        data: AssetsModel,
        position: Int
    ) {
        val binding = holder.binding
        binding.assetName.text = data.name
        binding.assetsType.text = when (data.type) {
            AssetsType.CREDIT -> holder.context.getString(R.string.type_credit)
            AssetsType.NORMAL -> holder.context.getString(R.string.type_normal)
            AssetsType.VIRTUAL -> holder.context.getString(R.string.type_virtual)
            AssetsType.FINANCIAL -> holder.context.getString(R.string.type_financial)
            AssetsType.BORROWER -> holder.context.getString(R.string.type_borrower)
            AssetsType.CREDITOR -> holder.context.getString(R.string.type_creditor)
        }
        holder.launch {
            ResourceUtils.getAssetDrawable(data, binding.assetIcon)
        }


    }

}