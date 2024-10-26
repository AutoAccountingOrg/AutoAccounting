package net.ankio.auto.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import net.ankio.auto.App
import net.ankio.auto.storage.Logger

/**
 * 本地广播助手类
 */
object LocalBroadcastHelper {

    const val ACTION_UPDATE_BILL = "update_bill" // 更新账单广播
    const val ACTION_APP_CHANGED = "app_changed"
    const val ACTION_UPDATE_FINISH = "update_finish" // 应用更新完成广播


    /**
     * 发送应用内广播
     *
     * @param action 广播的动作（唯一标识符）
     * @param extras 附加的数据
     */
    fun sendBroadcast(action: String, extras: Bundle? = null) {
        val intent = Intent(action).apply {
            extras?.let { putExtras(it) }
        }
        App.app.sendBroadcast(intent)
    }

    private val registeredReceivers = mutableMapOf<BroadcastReceiver, Boolean>()

    /**
     * 注册广播接收器
     *
     * @param action 广播的动作（唯一标识符）
     * @param onReceive 接收广播的回调函数
     */
    fun registerReceiver(action: String, onReceive: (String?, Bundle?) -> Unit): BroadcastReceiver {
        val receiver = object : SimpleBroadcastReceiver() {
            override fun onReceive(action: String?, extras: Bundle?) {
                onReceive(action, extras)
            }
        }
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            App.app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            App.app.registerReceiver(receiver, filter)
        }
        registeredReceivers[receiver] = true
        return receiver
    }

    /**
     * 注销广播接收器
     *
     * @param receiver 广播接收器
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        registeredReceivers[receiver]?.let {
            if (it) {
                runCatching {
                    App.app.unregisterReceiver(receiver)
                }.onSuccess {
                    registeredReceivers[receiver] = false
                }.onFailure {
                    Logger.e("Failed to unregister receiver: $receiver", it)
                }
            }
        } ?: Logger.w("Receiver not registered: $receiver")
    }

    /**
     * 简化的广播接收器
     */
    abstract class SimpleBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                onReceive(it.action, it.extras)
            }
        }

        abstract fun onReceive(action: String?, extras: Bundle?)
    }
}
