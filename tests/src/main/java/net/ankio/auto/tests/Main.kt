package net.ankio.auto.tests

import net.ankio.auto.tests.tests.LoginTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch

val ws = "ws://192.168.88.20:52045"
val list = listOf(
    LoginTest(),
)

fun main() {
    Print.success("Test started... total test cases: ${list.size}")

    val client = OkHttpClient()
    var pass = 0

    list.forEachIndexed { index, iTest ->
        Print.info("Test case ${index + 1} started...")
        val message = iTest.sendMessage()
        val expect = iTest.expect().trim()

        val latch = CountDownLatch(1)

        val request: Request = Request.Builder().url(ws).build()



        val ws: WebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                //Print.info("WebSocket connection opened")
                webSocket.send(message)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if(text.contains("\"type\" : \"auth\""))return
                if (text.trim() == expect) {
                    Print.success("Test case ${index + 1} passed")
                    pass++
                } else {
                    Print.error("Test case ${index + 1} failed, expected: $expect, received: ${text.trim()}")
                }
                webSocket.close(1000, null)
                latch.countDown()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                Print.error("Test case ${index + 1} failed with error: ${t.message}")
                latch.countDown()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                // Print.info("WebSocket is closing: code = $code, reason = $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                // Print.info("WebSocket closed: code = $code, reason = $reason")
            }
        })


        latch.await()
    }

    // 关闭客户端，确保所有资源被释放
    client.dispatcher.executorService.shutdown()

    Print.success("Test completed, passed: $pass, failed: ${list.size - pass}")
}
