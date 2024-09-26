package net.ankio.auto.ui.adapter

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.App
import net.ankio.auto.R
import net.ankio.auto.databinding.AdapterAppBinding
import net.ankio.auto.databinding.AdapterAutoAppBinding
import net.ankio.auto.databinding.AdapterBookBillBinding
import net.ankio.auto.storage.BackupUtils.Companion.SUFFIX
import net.ankio.auto.storage.BackupUtils.Companion.SUPPORT_VERSION
import net.ankio.auto.storage.Logger
import net.ankio.auto.ui.api.BaseAdapter
import net.ankio.auto.ui.api.BaseViewHolder
import net.ankio.auto.ui.utils.ResourceUtils
import net.ankio.auto.utils.BillTool
import net.ankio.auto.utils.CustomTabsHelper
import net.ankio.auto.utils.DateUtils
import org.ezbook.server.constant.BillType
import org.ezbook.server.db.model.BookBillModel

class BackupFileSelectorAdapter(
    private val list: MutableList<String>,
    private val callback: (uri:String)->Unit
) : BaseAdapter<AdapterAutoAppBinding, String>(
    AdapterAutoAppBinding::class.java, list
) {
    override fun onInitViewHolder(holder: BaseViewHolder<AdapterAutoAppBinding, String>) {
        val binding = holder.binding
        binding.root.setOnClickListener {
            val file = holder.item ?: return@setOnClickListener
            if (binding.checkbox.isEnabled){
                callback(file)
            }
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<AdapterAutoAppBinding,String>,
        data: String,
        position: Int
    ) {
        val binding = holder.binding

        val regex = Regex("backup_(.*)\\((\\d+)\\)_(\\d{10,13}).$SUFFIX")
        val matchResult = regex.matchEntire(data)
        binding.appPackageName.visibility = View.GONE
        binding.checkbox.isEnabled = false
        binding.appIcon.visibility = View.GONE
        if (matchResult != null) {
            val versionName = matchResult.groups[1]?.value?:""
            val supportVersion = matchResult.groups[2]?.value?.toIntOrNull()?:0
            val timestamp = matchResult.groups[3]?.value?.toLongOrNull()?:0

            if (supportVersion != SUPPORT_VERSION){
                binding.root.isClickable = false
                binding.appName.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceInverse))
              //  binding.appDesc.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceVariant))
                //     binding.appIcon.setImageDrawable(toGrayscale(icon))
            }else{
                binding.appName.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorPrimary))
                binding.appDesc.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorSecondary))
                binding.checkbox.isEnabled = true
                binding.root.isClickable = true
                // binding.appIcon.setImageDrawable(icon)
            }
            binding.appName.text = "$versionName($supportVersion)"
            binding.appDesc.text = DateUtils.getTime(timestamp)
        } else {
           binding.appName.text = "Unsupport Backup File"
           binding.appDesc.text = data
           binding.root.isClickable = false
            binding.appName.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceInverse))
          //  binding.appDesc.setTextColor(App.getThemeAttrColor(com.google.android.material.R.attr.colorSurfaceVariant))
         //  binding.appIcon.setImageDrawable(toGrayscale(icon))
        }

    }

}