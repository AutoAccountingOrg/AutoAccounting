package net.ankio.auto.xposed.hooks.wechat.hooks

import android.app.Application
import android.content.Intent
import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.logger.Logger
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils

class ChatUserHooker : PartHooker() {

    companion object {
        const val CHAT_USER = "hookerUser"
    }

    override fun hook() {


        val clazzUser  = AppRuntime.manifest.clazz("wechat_user",AppRuntime.classLoader)

        // public void convertFrom(Cursor cursor) {
        Hooker.after(clazzUser, "convertFrom",Hooker.loader("android.database.Cursor")) { param ->
            val obj = param.thisObject
            // field_conRemark
            // field_username
            // field_nickname
            val field_conRemark = XposedHelpers.getObjectField(obj, "field_conRemark") as? String

            val field_nickname = XposedHelpers.getObjectField(obj, "field_nickname") as? String

          //  val field_username = XposedHelpers.getObjectField(obj, "field_username") as? String

           // AppRuntime.manifest.logD("ChatUserHooker: $field_conRemark $field_username $field_nickname")

            val username = if (field_conRemark.isNullOrEmpty()) {
                field_nickname
            } else {
                field_conRemark
            }?:return@after
            DataUtils.set(CHAT_USER, username)

        }



    }


}
