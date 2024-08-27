package net.ankio.auto.storage

import net.ankio.auto.utils.Logger
import java.io.*
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipUtils {

    //========================解压===START===================================
    /**
     * 解压文件
     * 目前支持范围：无文件夹压缩包和带文件夹压缩包
     */
    fun unzip(zipFilePath: String, desDirectory: String, callback: ((String) -> Unit)? = null) {
        val desDir = File(desDirectory)
        if (!desDir.exists()) {
            val mkdirSuccess = desDir.mkdirs()
            if (!mkdirSuccess) {
                Logger.e("创建文件夹失败:$desDirectory")
                return
            }
        }
        ZipInputStream(FileInputStream(zipFilePath), Charset.defaultCharset()).use { zipInputStream ->
            var zipEntry: ZipEntry? = zipInputStream.nextEntry
            while (zipEntry != null) {
                val unzipFilePath = desDirectory + File.separator + zipEntry.name
                callback?.invoke(zipEntry.name)
                if (zipEntry.isDirectory) {
                    mkdir(File(unzipFilePath))
                } else {
                    val file = File(unzipFilePath)
                    mkdir(file.parentFile!!)
                    BufferedOutputStream(FileOutputStream(unzipFilePath)).use { bufferedOutputStream ->
                        val bytes = ByteArray(1024)
                        var readLen: Int
                        while (zipInputStream.read(bytes).also { readLen = it } > 0) {
                            bufferedOutputStream.write(bytes, 0, readLen)
                        }
                    }
                }
                zipInputStream.closeEntry()
                zipEntry = zipInputStream.nextEntry
            }
        }
    }

    /**
     * 创建文件夹
     */
    private fun mkdir(file: File) {
        if (!file.exists()) {
            file.mkdirs()
        }
    }
    //========================解压===END===================================

    //========================压缩===START=================================
    private fun zipAll(directory: String, zipFile: String) {
        val sourceFile = File(directory)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use {
            zipFiles(it, sourceFile, "")
        }
    }

    private fun zipFiles(zipOut: ZipOutputStream, sourceFile: File, parentDirPath: String) {
        val data = ByteArray(2048)
        for (f in sourceFile.listFiles()!!) {
            if (f.isDirectory) {
                val path = parentDirPath + f.name + File.separator
                val entry = ZipEntry(path)
                entry.time = f.lastModified()
                entry.size = f.length()
                Logger.i("Adding Directory: $path")
                zipOut.putNextEntry(entry)
                zipFiles(zipOut, f, path)
            } else {
                if (!f.name.contains(".zip")) {
                    FileInputStream(f).use { fi ->
                        BufferedInputStream(fi).use { origin ->
                            val path = parentDirPath + f.name
                            Logger.i("Adding file: $path")
                            val entry = ZipEntry(path)
                            entry.time = f.lastModified()
                            entry.size = f.length()
                            zipOut.putNextEntry(entry)
                            while (true) {
                                val readBytes = origin.read(data)
                                if (readBytes == -1) {
                                    break
                                }
                                zipOut.write(data, 0, readBytes)
                            }
                        }
                    }
                }
            }
        }
    }
    //========================压缩===END=================================
}
