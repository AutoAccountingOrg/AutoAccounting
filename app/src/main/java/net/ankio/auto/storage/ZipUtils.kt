package net.ankio.auto.storage

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.File
import io.github.oshai.kotlinlogging.KotlinLogging

object ZipUtils {

    private val logger = KotlinLogging.logger(this::class.java.name)

    /**
     * 压缩目录或文件，支持可选密码
     * @param sourceFile 目标文件或文件夹
     * @param zipFile 输出的zip文件路径
     * @param password 为空表示不加密
     * @param excludeRootDir 压缩目录时是否排除根目录本身，仅压缩目录内容
     */
    fun zipAll(
        sourceFile: File,
        zipFile: String,
        password: String? = null,
        excludeRootDir: Boolean = false
    ) {
        try {
            val zip = if (!password.isNullOrEmpty())
                ZipFile(zipFile, password.toCharArray())
            else
                ZipFile(zipFile)

            val params = ZipParameters().apply {
                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                }
            }

            when {
                sourceFile.isFile -> {
                    // 单文件直接添加
                    zip.addFile(sourceFile, params)
                }

                sourceFile.isDirectory && excludeRootDir -> {
                    // 压缩目录内容，排除根目录
                    sourceFile.listFiles()?.forEach { child ->
                        if (child.isFile) {
                            zip.addFile(child, params)
                        } else if (child.isDirectory) {
                            zip.addFolder(child, params)
                        }
                    }
                }

                sourceFile.isDirectory -> {
                    // 压缩整个目录（包含根目录）
                    zip.addFolder(sourceFile, params)
                }
            }
        } catch (e: Exception) {
            logger.error { "Zip failed: ${e.message}" }
        }
    }

    /**
     * 解压，自动判断是否加密
     * @param zipFilePath zip文件路径
     * @param desDirectory 解压到哪里
     * @param password 可选，若为null或空自动尝试无密码解压
     */
    fun unzip(
        zipFilePath: String,
        desDirectory: String,
        password: String? = null,
        callback: ((String) -> Unit)? = null
    ) {
        try {
            val zipFile = ZipFile(zipFilePath)
            if (zipFile.isEncrypted && !password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }
            zipFile.extractAll(desDirectory)
            zipFile.fileHeaders.forEach { callback?.invoke(it.fileName) }
        } catch (e: Exception) {
            logger.error { "Unzip failed: ${e.message}" }
        }
    }
}
