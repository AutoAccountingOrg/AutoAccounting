package net.ankio.shortcuts.shell

import java.io.*
import java.util.concurrent.TimeUnit

class RootShell {

    private var process: Process? = null
    private var writer: BufferedWriter? = null

    /**
     * 启动 su shell
     */
    fun openShell(): Boolean {
        return try {
            val builder = ProcessBuilder("su").apply {
                redirectErrorStream(true) // stderr 合并进 stdout
            }
            process = builder.start()
            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    /**
     * 执行命令并返回标准输出（不建议执行交互性命令）
     */
    fun execute(cmd: String, timeoutSec: Long = 10): String {
        if (process == null || writer == null) return ""

        val output = ByteArrayOutputStream()
        val inputStream = process!!.inputStream

        return try {
            val eofMarker = "--EOF--${System.currentTimeMillis()}"
            writer!!.write("$cmd\n")
            writer!!.write("echo $eofMarker\n")
            writer!!.flush()

            val buffer = ByteArray(1024)
            val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSec)

            while (System.currentTimeMillis() < deadline) {
                if (inputStream.available() > 0) {
                    val len = inputStream.read(buffer)
                    if (len > 0) {
                        output.write(buffer, 0, len)
                        if (output.toString().contains(eofMarker)) break
                    }
                } else {
                    Thread.sleep(50) // 等待数据到达
                }
            }

            // 去除 EOF 标记后的部分
            output.toString().replaceAfter(eofMarker, "").replace(eofMarker, "").trim()
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 关闭 shell
     */
    fun close(): Boolean {
        return try {
            writer?.write("exit\n")
            writer?.flush()
            process?.destroy()
            writer?.close()
            process = null
            writer = null
            true
        } catch (e: IOException) {
            false
        }
    }
}
