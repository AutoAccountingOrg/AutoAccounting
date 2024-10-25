package net.ankio.auto.xposed.hooks.wechat.hooks

import android.app.Application
import net.ankio.auto.xposed.core.api.HookerManifest
import net.ankio.auto.xposed.core.api.PartHooker
import net.ankio.auto.xposed.core.hook.Hooker
import net.ankio.auto.xposed.core.utils.DataUtils

class ChatUserHooker : PartHooker() {

    companion object {
        const val CHAT_USER = "hookerUser"
    }

    override fun hook(
        hookerManifest: HookerManifest,
        application: Application?,
        classLoader: ClassLoader
    ) {
        val clazz = Hooker.loader("com.tencent.mm.ui.chatting.ChattingUIFragment")

        // Hook into the setMMTitle method to capture the chat username
        Hooker.after(clazz, "setMMTitle") { param ->
            val username = param.args[0] as? String
            if (username != null) {
                hookerManifest.logD("ChatUserHooker: $username")
                DataUtils.set(CHAT_USER, username)
            }
        }
    }
}
