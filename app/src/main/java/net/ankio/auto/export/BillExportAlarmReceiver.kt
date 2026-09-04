package net.ankio.auto.export

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BillExportAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            BillExportScheduler.configure(context)
            return
        }
        if (intent.action == BillExportScheduler.ACTION_ALARM) {
            BillExportScheduler.handleAlarm(
                context,
                intent.getIntExtra(BillExportWorker.KEY_DAY_OFFSET, 0),
            )
        }
    }
}
