package net.ankio.auto.ui.activity

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zackratos.ultimatebarx.ultimatebarx.addNavigationBarBottomPadding
import net.ankio.auto.R
import net.ankio.auto.databinding.AboutDialogBinding
import net.ankio.auto.databinding.ActivityMainBinding
import rikka.html.text.toHtml


class MainActivity : BaseActivity() {

    //视图绑定
    private lateinit var binding: ActivityMainBinding

    private lateinit var fragmentContainerView: FragmentContainerView
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fragmentContainerView = binding.navHostFragment
        bottomNavigationView = binding.bottomNavigation

        toolbarLayout = binding.toolbarLayout
        toolbar = binding.toolbar
        bottomNavigationView.addNavigationBarBottomPadding()
        scrollView = binding.scrollView
        navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?)!!

        NavigationUI.setupWithNavController(bottomNavigationView, navHostFragment.navController)

        // 添加 Navigate 跳转监听，如果参数不带 ShowAppBar 将不显示底部导航栏
        navHostFragment.navController.addOnDestinationChangedListener { _, navDestination, _ ->
            bottomNavigationView.menu.findItem(R.id.homeFragment).setIcon(R.drawable.unselect_home);
            bottomNavigationView.menu.findItem(R.id.dataFragment).setIcon(R.drawable.unselect_data);
            bottomNavigationView.menu.findItem(R.id.settingFragment).setIcon(R.drawable.unselect_setting);
            bottomNavigationView.menu.findItem(R.id.ruleFragment).setIcon(R.drawable.unselect_rule);
            bottomNavigationView.menu.findItem(R.id.orderFragment).setIcon(R.drawable.unselect_order);

            when (navDestination.id) {
                R.id.homeFragment -> {
                    toolbar.title = getString(R.string.title_home)
                    bottomNavigationView.menu.findItem(R.id.homeFragment).setIcon(R.drawable.select_home);
                }
                R.id.dataFragment -> {
                    toolbar.title = getString(R.string.title_data)
                    bottomNavigationView.menu.findItem(R.id.dataFragment).setIcon(R.drawable.select_data);
                }
                R.id.settingFragment -> {
                    toolbar.title = getString(R.string.title_setting)
                    bottomNavigationView.menu.findItem(R.id.settingFragment).setIcon(R.drawable.select_setting);
                }
                R.id.ruleFragment -> {
                    toolbar.title = getString(R.string.title_rule)
                    bottomNavigationView.menu.findItem(R.id.ruleFragment).setIcon(R.drawable.select_rule);
                }

                R.id.orderFragment->{
                    toolbar.title = getString(R.string.title_order)
                    bottomNavigationView.menu.findItem(R.id.orderFragment).setIcon(R.drawable.select_order);
                }

            }
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.more -> {
                    val binding = AboutDialogBinding.inflate(LayoutInflater.from(this), null, false)
                    binding.sourceCode.movementMethod = LinkMovementMethod.getInstance()
                    binding.sourceCode.text = getString(
                        R.string.about_view_source_code,
                        "<b><a href=\"https://github.com/Auto-Accounting/AutoAccounting\n\">GitHub</a></b>"
                    ).toHtml()

                    binding.versionName.text = packageManager.getPackageInfo(packageName, 0).versionName
                    MaterialAlertDialogBuilder(this)
                        .setView(binding.root)
                        .show()
                    true
                }
                else -> false
            }

        }
        onViewCreated()
    }




}