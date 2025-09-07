package net.ankio.auto.xposed.hooks.wechat.hooks

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils

class ChatUserHooker : PartHooker() {

    companion object {
        const val CHAT_USER = "hookerUser"
        private const val DURATION_SECONDS = 300L
        // 内存缓存用户数据
        val users = hashMapOf<String,String>()

        fun get(wx: String): String {
            return users[wx]?:DataUtils.get(CHAT_USER)
        }

    }




    override fun hook() {


        val clazzUser  = AppRuntime.manifest.clazz("wechat_user")

        // public void convertFrom(Cursor cursor) {
        Hooker.after(clazzUser, "convertFrom",Hooker.loader("android.database.Cursor")) { param ->
            val obj = param.thisObject
            // field_conRemark
            // field_username
            // field_nickname
            val field_conRemark = XposedHelpers.getObjectField(obj, "field_conRemark") as? String

            val field_nickname = XposedHelpers.getObjectField(obj, "field_nickname") as? String

            val field_username = XposedHelpers.getObjectField(obj, "field_username") as? String

           // AppRuntime.manifest.logD("ChatUserHooker: $field_conRemark $field_username $field_nickname")

            if(field_username.isNullOrEmpty()) return@after

            val username = if (field_conRemark.isNullOrEmpty()) {
                field_nickname
            } else {
                field_conRemark
            }?:return@after
            users[field_username] = username
            if (field_nickname != null) users[field_nickname] = username

            AppRuntime.memoryCache.put(CHAT_USER, username, DURATION_SECONDS)

        }



    }


}
