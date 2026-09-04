package net.ankio.auto.export

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import net.ankio.auto.utils.PrefManager
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object BillExportScheduler {
    private const val PERIODIC_NAME = "bill-export-hourly"
    private const val IMMEDIATE_NAME = "bill-export-immediate"
    const val ACTION_ALARM = "net.ankio.auto.BILL_EXPORT_ALARM"

    fun configure(context: Context) {
        val work = WorkManager.getInstance(context)
        if (!PrefManager.billExportEnabled) {
            work.cancelUniqueWork(PERIODIC_NAME)
            cancelAlarms(context)
            return
        }
        val periodic = PeriodicWorkRequestBuilder<BillExportWorker>(1, TimeUnit.HOURS)
            .build()
        work.enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        scheduleAlarm(context, 21, 20, 0, 2120)
        scheduleAlarm(context, 0, 30, -1, 30)
    }

    fun enqueueImmediate(context: Context, dayOffset: Int = 0) {
        if (!PrefManager.billExportEnabled) return
        val request = OneTimeWorkRequestBuilder<BillExportWorker>()
            .setInputData(Data.Builder().putInt(BillExportWorker.KEY_DAY_OFFSET, dayOffset).build())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun handleAlarm(context: Context, dayOffset: Int) {
        enqueueImmediate(context, dayOffset)
        configure(context)
    }

    private fun scheduleAlarm(context: Context, hour: Int, minute: Int, offset: Int, requestCode: Int) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val pending = alarmIntent(context, offset, requestCode)
        val millis = next.toInstant().toEpochMilli()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    private fun alarmIntent(context: Context, offset: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, BillExportAlarmReceiver::class.java)
            .setAction(ACTION_ALARM)
            .putExtra(BillExportWorker.KEY_DAY_OFFSET, offset)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelAlarms(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(alarmIntent(context, 0, 2120))
        manager.cancel(alarmIntent(context, -1, 30))
    }
}
