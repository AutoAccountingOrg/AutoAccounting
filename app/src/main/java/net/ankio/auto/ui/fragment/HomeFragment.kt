package net.ankio.auto.ui.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.MaterialColors
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

    private fun setActive(text: String, @AttrRes backgroundColor:Int, @AttrRes textColor:Int, @DrawableRes drawable:Int){
        binding.active2.setBackgroundColor(getThemeAttrColor(backgroundColor))
        binding.imageView2.setImageDrawable(
            AppCompatResources.getDrawable(
                requireActivity(),
                drawable
            )
        )
        binding.msgLabel2.text = text
        binding.imageView2.setColorFilter(getThemeAttrColor(textColor))
        binding.msgLabel2.setTextColor(getThemeAttrColor(textColor))
    }

    private fun refreshStatus(){
        if(!ActiveUtils.getActiveAndSupportFramework()){
            setActive(getString(R.string.not_work,ActiveUtils.errorMsg(requireContext())),com.google.android.material.R.attr.colorErrorContainer,com.google.android.material.R.attr.colorOnErrorContainer, R.drawable.ic_error)
        }else{
            setActive(getString(R.string.work,ActiveUtils.errorMsg(requireContext())),com.google.android.material.R.attr.colorPrimary,com.google.android.material.R.attr.colorOnPrimary,R.drawable.ic_success)
        }
    }

}