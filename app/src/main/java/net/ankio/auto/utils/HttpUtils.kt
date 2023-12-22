package net.ankio.auto.utils
import android.util.Log
import okhttp3.*
import java.io.IOException

class HttpUtils {
    private val client = OkHttpClient()

    // 封装GET请求
    fun get(url: String, headers: Headers? = null, onSuccess: (body:String)->Unit, onError: (url:String,errorInfo:String)->Unit) {
        Log.e("Github请求",url)
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
                    onSuccess(responseBody)
                } else {
                    onError(url,response.body?.string()?:"")
                    Log.e("Github",url)
                    Log.e("Github",response.body?.string()?:"")
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                onError(url,e.message.toString())
            }
        })
    }

    // 封装POST请求
    fun post(url: String, requestBody: RequestBody, headers: Headers? = null, onSuccess: (String)->Unit, onError: (url:String,errorInfo:String)->Unit) {
        Log.e("Github请求",url)
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
                    onSuccess(responseBody)
                } else {
                    onError(url,response.body?.string()?:"")
                    Log.e("Github",url)
                    Log.e("Github",response.body?.string()?:"")
                }
                response.close()
            }

            override fun onFailure(call: Call, e: IOException) {
                onError(url,e.message.toString())
            }
        })
    }
}
