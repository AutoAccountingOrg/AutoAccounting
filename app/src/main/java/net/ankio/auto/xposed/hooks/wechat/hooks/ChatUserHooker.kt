package net.ankio.auto.xposed.hooks.wechat.hooks

import android.app.Application
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.AppRuntime
import net.ankio.auto.xposed.core.utils.DataUtils

class ChatUserHooker : PartHooker() {

    companion object {
        const val CHAT_USER = "hookerUser"
    }

    override fun hook() {
        val clazz = Hooker.loader("com.tencent.mm.ui.chatting.ChattingUIFragment")
        Hooker.after(clazz, "setMMTitle", String::class.java) { param ->
            val username = param.args[0] as? String
            if (username != null) {
                AppRuntime.manifest.logD("ChatUserHooker: $username")
                DataUtils.set(CHAT_USER, username)
            }
        }
    }
}
