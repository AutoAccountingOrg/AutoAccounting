package net.ankio.auto

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.quickersilver.themeengine.ThemeEngine
import java.util.*


open class App : Application() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        ThemeEngine.applyToActivities(this)
    }


}