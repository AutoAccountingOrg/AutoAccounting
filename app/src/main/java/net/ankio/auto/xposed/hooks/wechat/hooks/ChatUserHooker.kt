package net.ankio.auto.xposed.hooks.wechat.hooks

import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.hooks.wechat.models.WechatUserModel

class ChatUserHooker : PartHooker() {


    override fun hook() {


        val clazzUser = WechatUserModel.clazz()

        // public void convertFrom(Cursor cursor) {
        Hooker.after(clazzUser, "convertFrom", Hooker.loader("android.database.Cursor")) { param ->
            val obj = param.thisObject
            WechatUserModel.parse(obj)

        }

    }


}
