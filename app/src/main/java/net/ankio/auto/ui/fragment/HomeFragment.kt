package net.ankio.auto.ui.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors
import com.quickersilver.themeengine.ThemeEngine
import net.ankio.auto.R
import net.ankio.auto.databinding.FragmentHomeBinding
import net.ankio.auto.utils.ActiveUtils


/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        binding.logCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        binding.grouptCard.setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(requireContext()))
        refreshStatus()
        return binding.root
    }



    override fun onResume() {
        super.onResume()
        refreshStatus()
    }
    /**
     * 获取主题色
     */
    private fun getThemeAttrColor( @AttrRes attrResId: Int): Int {
        return MaterialColors.getColor(ContextThemeWrapper(requireContext(), ThemeEngine.getInstance(requireContext()).getTheme()), attrResId, Color.WHITE)
    }
    private fun setActive(@ColorInt backgroundColor:Int, @ColorInt textColor:Int, @DrawableRes drawable:Int){
        binding.active2.setBackgroundColor(backgroundColor)
        binding.imageView2.setImageDrawable(
            AppCompatResources.getDrawable(
                requireActivity(),
                drawable
            )
        )
        val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        val names = versionName.split("-")
        binding.msgLabel2.text = names[0].trim()
        binding.msgLabel3.text = getString(R.string.releaseInfo)
        binding.imageView2.setColorFilter(textColor)
        binding.msgLabel2.setTextColor(textColor)
    }

    private fun refreshStatus(){
        if(!ActiveUtils.getActiveAndSupportFramework()){
            setActive(SurfaceColors.SURFACE_3.getColor(requireContext()),getThemeAttrColor(com.google.android.material.R.attr.colorPrimary), R.drawable.ic_error)
        }else{
            setActive(getThemeAttrColor(com.google.android.material.R.attr.colorPrimary),getThemeAttrColor(com.google.android.material.R.attr.colorOnPrimary),R.drawable.ic_success)
        }
    }

}