package net.ankio.auto.storage

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    //========================解压===START===================================
    /**
     * 解压文件
     * 支持无文件夹和带文件夹的压缩包
     */
    fun unzip(zipFilePath: String, desDirectory: String, callback: ((String) -> Unit)? = null) {
        val desDir = File(desDirectory)
        if (!desDir.exists() && !desDir.mkdirs()) {
            Logger.e("创建文件夹失败: $desDirectory")
            return
        }
        try {
            ZipInputStream(
                FileInputStream(zipFilePath),
                Charset.defaultCharset()
            ).use { zipInputStream ->
                var zipEntry: ZipEntry? = zipInputStream.nextEntry
                while (zipEntry != null) {
                    val unzipFilePath = Paths.get(desDirectory, zipEntry.name).toString()
                    callback?.invoke(zipEntry.name)

                    if (zipEntry.isDirectory) {
                        Files.createDirectories(Paths.get(unzipFilePath))
                    } else {
                        val file = File(unzipFilePath)
                        Files.createDirectories(file.parentFile.toPath())
                        BufferedOutputStream(FileOutputStream(file)).use { bufferedOutputStream ->
                            val buffer = ByteArray(8192)  // 优化缓冲区大小
                            var readLen: Int
                            while (zipInputStream.read(buffer).also { readLen = it } > 0) {
                                bufferedOutputStream.write(buffer, 0, readLen)
                            }
                        }
                    }
                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }
            }
        } catch (e: IOException) {
            Logger.e("解压失败: ${e.message}")
        }
    }
    //========================解压===END===================================

    //========================压缩===START=================================
    fun zipAll(sourceFile: File, zipFile: String) {
        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                zipFiles(zipOut, sourceFile, "")
            }
        } catch (e: IOException) {
            Logger.e("压缩失败: ${e.message}")
        }
    }

    private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirPath: String) {
        val buffer = ByteArray(8192)  // 优化缓冲区大小
        sourceFile.listFiles()?.forEach { file ->
            val zipPath = parentDirPath + file.name + if (file.isDirectory) File.separator else ""
            Logger.d("Adding ${if (file.isDirectory) "Directory" else "file"}: $zipPath")

            try {
                zipOut.putNextEntry(ZipEntry(zipPath).apply {
                    time = file.lastModified()
                    size = if (file.isFile) file.length() else 0
                })

                if (file.isDirectory) {
                    zipFiles(zipOut, file, zipPath)
                } else {
                    FileInputStream(file).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            var readLen: Int
                            while (origin.read(buffer).also { readLen = it } != -1) {
                                zipOut.write(buffer, 0, readLen)
                            }
                        }
                    }
                }
                zipOut.closeEntry()
            } catch (e: IOException) {
                Logger.e("压缩文件失败: ${e.message}")
            }
        }
    }
    //========================压缩===END===================================
}
