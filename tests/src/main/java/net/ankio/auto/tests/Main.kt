package net.ankio.auto.tests

import net.ankio.auto.tests.tests.AppDataTest
import net.ankio.auto.tests.tests.LoginTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.CountDownLatch

val ws = "ws://192.168.88.20:52045"
val list = listOf(
    LoginTest(),
    AppDataTest()
)
fun test(index: Int, name:String,message: String, expect: String) {
    Print.warning("Test case $name started...")

    val latch = CountDownLatch(1)

    val request: Request = Request.Builder().url(ws).build()



    val ws: WebSocket = client.newWebSocket(request, object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
            //Print.info("WebSocket connection opened")
            webSocket.send(message)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val receivedText = text.trim()
            if(text.contains("\"type\" : \"auth\""))return
            if (expect == "" || receivedText == expect) {
                Print.success("Test case $name passed, received: $receivedText")
                pass++
            } else {
                Print.error("Test case $name failed, expected: $expect, received: $receivedText")
            }
            webSocket.close(1000, null)
            latch.countDown()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
            Print.error("Test case $name failed with error: ${t.message}")
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
val client = OkHttpClient()
var total = 0;
var pass = 0;
fun main() {
    Print.success("Test started... total test cases: ${list.size}")



    list.forEachIndexed { index, iTest ->
        Print.info("============Test case ${index + 1} => ${iTest.name()} started...============")
        val cases = iTest.cases()
        total += cases.size
        cases.forEach {
            test(index,iTest.name()+" - "+it.name, it.message.trim(), it.expect.trim())
        }

    }

    // 关闭客户端，确保所有资源被释放
    client.dispatcher.executorService.shutdown()

    Print.success("Test completed, passed: $pass, failed: ${list.size - pass}")
}
