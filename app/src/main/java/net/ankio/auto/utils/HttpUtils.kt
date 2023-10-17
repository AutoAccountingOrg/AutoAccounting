package net.ankio.auto.utils
import android.util.Log
import okhttp3.*
import java.io.IOException

class HttpUtils {
    private val client = OkHttpClient()

    interface CallbackListener {
        fun onSuccess(response: String)
        fun onFailure(e: IOException)
    }

    // 封装GET请求
    fun get(url: String, headers: Headers? = null, listener: CallbackListener) {
        val requestBuilder = Request.Builder()
            .url(url)

        // 添加可选的headers
        headers?.let {
            requestBuilder.headers(it)
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    listener.onSuccess(responseBody)

                } else {
                    listener.onFailure(IOException("Request failed"))
                    Log.e("Github",response.body?.toString()?:"")
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure(e)
            }
        })
    }

    // 封装POST请求
    fun post(url: String, requestBody: RequestBody, headers: Headers? = null, listener: CallbackListener) {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)

        // 添加可选的headers
        headers?.let {
            requestBuilder.headers(it)
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    listener.onSuccess(responseBody)
                } else {
                    listener.onFailure(IOException("Request failed: "+response.code+" "+ (response.body?.string() ?: "")))
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                listener.onFailure(e)
            }
        })
    }
}
