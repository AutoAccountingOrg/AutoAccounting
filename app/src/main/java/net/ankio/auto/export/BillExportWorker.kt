package net.ankio.auto.export

import android.content.Context
import android.os.SystemClock
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.ankio.auto.storage.Logger
import java.time.LocalDate
import java.time.ZoneId

class BillExportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val started = SystemClock.elapsedRealtime()
        val offset = inputData.getInt(KEY_DAY_OFFSET, 0)
        val day = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(offset.toLong())
        val result = BillExporter.exportDay(day)
        return result.fold(
            onSuccess = {
                Logger.d(
                    "账单导出完成: day=$day states=${it.stateCounts} pulled=${it.pulled} " +
                        "posted=${it.posted} failed=0 elapsed_ms=${SystemClock.elapsedRealtime() - started}"
                )
                Result.success()
            },
            onFailure = { error ->
                val failure = error as? BillExportFailure
                Logger.e(
                    "账单导出失败: day=$day states=${failure?.stateCounts.orEmpty()} " +
                        "posted=${failure?.posted ?: 0} failed=1 " +
                        "error=${failure?.kind ?: "unknown"} " +
                        "elapsed_ms=${SystemClock.elapsedRealtime() - started}"
                )
                Result.failure()
            },
        )
    }

    companion object {
        const val KEY_DAY_OFFSET = "day_offset"
    }
}
