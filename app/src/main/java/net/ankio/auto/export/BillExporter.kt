package net.ankio.auto.export

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ankio.auto.http.RequestsUtils
import net.ankio.auto.http.api.BillAPI
import net.ankio.auto.utils.PrefManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.ezbook.server.constant.BillState
import org.ezbook.server.db.model.BillInfoModel
import org.ezbook.server.tools.runCatchingExceptCancel
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class BillExportResult(val stateCounts: Map<String, Int>, val posted: Int) {
    val pulled: Int get() = stateCounts.values.sum()
}

class BillExportFailure(
    val kind: String,
    val stateCounts: Map<String, Int>,
    val posted: Int,
    cause: Throwable,
) : Exception(kind, cause)

object BillExporter {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
    private val states = listOf(BillState.Synced, BillState.Edited, BillState.Wait2Edit)

    suspend fun exportDay(day: LocalDate): Result<BillExportResult> = withContext(Dispatchers.IO) {
        val counts = linkedMapOf<String, Int>()
        var posted = 0
        runCatchingExceptCancel {
            check(PrefManager.billExportEnabled) { "disabled" }
            val endpoint = PrefManager.billExportUrl.trim()
            val token = PrefManager.billExportToken.trim()
            check(endpoint.startsWith("https://")) { "endpoint_must_be_https" }
            check(token.isNotEmpty()) { "token_missing" }

            val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

            // Pull all three states before posting anything. An empty successful list is valid;
            // any failed local call aborts the entire run and leaves NAS files untouched.
            val bills = mutableListOf<BillInfoModel>()
            for (state in states) {
                val stateBills = BillAPI.exportList(state, start, end).getOrThrow()
                counts[state.name] = stateBills.size
                bills += stateBills
            }

            for (bill in bills) {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .post(gson.toJson(bill).toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "nas_http_${response.code}" }
                }
                posted++
            }
            BillExportResult(counts.toMap(), posted)
        }.recoverCatching { error ->
            throw BillExportFailure(shortError(error), counts.toMap(), posted, error)
        }
    }

    private fun shortError(error: Throwable): String = when {
        error.message in setOf("disabled", "endpoint_must_be_https", "token_missing") -> error.message!!
        error.message?.startsWith("nas_http_") == true -> error.message!!
        error is RequestsUtils.HttpException -> "local_http_${error.code}"
        error is java.io.IOException -> "network_io"
        else -> error.javaClass.simpleName.take(40)
    }
}
