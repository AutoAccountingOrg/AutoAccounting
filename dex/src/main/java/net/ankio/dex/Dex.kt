package net.ankio.dex

import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.DexFile
import java.io.BufferedInputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.zip.ZipFile

object Dex {
    fun getAllDexFiles(file: String): List<DexFile> {
        val dexFiles = mutableListOf<DexBackedDexFile>()
        ZipFile(file).use { zipFile ->
            // Iterate over all entries in the ZIP file
            for (entry in zipFile.entries()) {
                if (entry.name.endsWith(".dex")) {
                    // Load the DEX file
                    val dexFile =
                        DexBackedDexFile.fromInputStream(
                            null,
                            BufferedInputStream(zipFile.getInputStream(entry)),
                        )
                    dexFiles.add(dexFile)
                }
            }
        }
        return dexFiles
    }

    fun findClazz(
        path: String,
        classLoader: ClassLoader,
        rules: List<Clazz>,
    ): HashMap<String, String> {
        val results = HashMap<String, String>()
        for (dexFile in getAllDexFiles(path)) {
            if (results.size == rules.size)
                {
                    // 所有需要查找的元素全部找到
                    break
                }

            for (classDef in dexFile.classes) {
                if (results.size == rules.size)
                    {
                        // 所有需要查找的元素全部找到
                        break
                    }
                var className = classDef.type
                className = className.substring(1, className.length - 1).replace('/', '.')
                //   println("class -> "+className)
                rules.forEach { itClazz ->
                    if (results.containsKey(itClazz.name))return@forEach
                    // 判断包名规则有要求吗？有要求就使用正则匹配
                    val condition1 =
                        if (itClazz.nameRule.isNotEmpty()) {
                            Regex(itClazz.nameRule).matches(className)
                        } else {
                            true
                        }

                    if (condition1)
                        {
                            val clazz = runCatching {  classLoader.loadClass(className) }.getOrNull() ?: return@forEach

                            val condition4 =
                                if (itClazz.type.isNotEmpty())
                                    {
                                        when (itClazz.type) {
                                            "interface" -> clazz.isInterface
                                            "abstract" -> Modifier.isAbstract(clazz.modifiers)
                                            "enum" -> clazz.isEnum
                                            else -> true
                                        }
                                    } else {
                                    true
                                }

                            if (itClazz.type == "enum" && clazz.isEnum && condition4)
                                {
                                    val result =
                                        itClazz.fields.map { field ->
                                            clazz.enumConstants.any { enumConstant ->
                                                (enumConstant as Enum<*>).name == field.name
                                            }
                                        }.all { it }
                                    if (result)
                                        {
                                            results[itClazz.name] = className
                                            return@forEach
                                        }
                                }

                            if (condition4)
                                {
                                    val condition2 =
                                        if (itClazz.fields.isNotEmpty()) {
                                            // 这里应该是判断字段是否存在吧？
                                            itClazz.fields.map { field ->
                                                findFieldIsExist(clazz, field)
                                            }.all { it }
                                        } else {
                                            true
                                        }
                                    val condition3 =
                                        if (itClazz.methods.isNotEmpty()) {
                                            itClazz.methods.map { method ->
                                                findMethodIsExist(clazz, method)
                                            }.all { it }
                                        } else {
                                            true
                                        }
                                    if (condition2 && condition3) {
                                        results[itClazz.name] = className
                                    }
                                }
                        }
                }
            }
        }

        return results
    }

    /**
     * 查找某个字段，只能根据类型查找咯
     */
    fun findField(
        clazz: Class<*>,
        field: ClazzField,
    ): String {
        // 判断指定要求的字段是否在class里面
        val fields: Array<Field> = clazz.declaredFields
        for (f in fields) {
            f.isAccessible = true
            if (f.type.name == field.type) {
                return field.name
            }
        }
        return ""
    }

    /**
     * 判断某个字段是否存在
     */
    private fun findFieldIsExist(
        clazz: Class<*>,
        field: ClazzField,
    ): Boolean {
        if (field.name.isEmpty() && field.type.isEmpty()) {
            return false
        }
        // 判断指定要求的字段是否在class里面
        val fields: Array<Field> = clazz.declaredFields
        for (f in fields) {
            f.isAccessible = true

            val condition1 =
                if (field.name.isNotEmpty()) {
                    f.name == field.name
                } else {
                    true
                }

            val condition2 =
                if (field.type.isNotEmpty()) {
                    f.type.name == field.type
                } else {
                    true
                }

            return condition1 && condition2
        }
        return false
    }

    /**
     * 查找某个方法
     */
    fun findMethod(
        clazz: Class<*>,
        clazzMethod: ClazzMethod,
    ): String {
        // 判断指定要求的字段是否在class里面
        val methods = clazz.declaredMethods
        for (method in methods) {
            val condition1 =
                if (clazzMethod.regex.isNotEmpty()) {
                    Regex(clazzMethod.regex).matches(method.name)
                } else {
                    true
                }
            val condition2 =
                if (clazzMethod.returnType.isNotEmpty()) {
                    method.returnType.name == clazzMethod.returnType
                } else {
                    true
                }

            val condition3 =
                if (clazzMethod.modifiers.isNotEmpty()) {
                    method.modifiers.toString() == clazzMethod.modifiers
                } else {
                    true
                }

            val condition4 =
                if (clazzMethod.parameters.isNotEmpty()) {
                    val parameters = method.parameters
                    if (parameters.size == clazzMethod.parameters.size) {
                        parameters.mapIndexed { index, parameter ->
                            parameter.type.name == clazzMethod.parameters[index].type
                        }.all { it }
                    } else {
                        false
                    }
                } else {
                    true
                }

            if (condition1 && condition2 && condition3 && condition4) {
                return method.name
            }
        }
        return ""
    }

    /**
     * 判断某个方法是否在给定的clazz里面
     */
    private fun findMethodIsExist(
        clazz: Class<*>,
        clazzMethod: ClazzMethod,
    ): Boolean {
        if (clazzMethod.name.isEmpty() && clazzMethod.returnType.isEmpty() && clazzMethod.modifiers.isEmpty() && clazzMethod.parameters.isEmpty()) {
            return false
        }

        // 判断指定要求的字段是否在class里面
        val methods = clazz.methods

        for (method in methods) {
            val condition1 =
                if (clazzMethod.name.isNotEmpty()) {
                    method.name == clazzMethod.name
                } else {
                    true
                }

            val condition2 =
                if (clazzMethod.returnType.isNotEmpty()) {
                    method.returnType.name == clazzMethod.returnType
                } else {
                    true
                }

            val condition3 =
                if (clazzMethod.modifiers.isNotEmpty()) {
                    method.modifiers.toString() == clazzMethod.modifiers
                } else {
                    true
                }

            val condition4 =
                if (clazzMethod.parameters.isNotEmpty()) {
                    val parameters = method.parameters
                    if (parameters.size == clazzMethod.parameters.size) {
                        parameters.mapIndexed { index, parameter ->
                            parameter.type.name == clazzMethod.parameters[index].type
                        }.all { it }
                    } else {
                        false
                    }
                } else {
                    true
                }

            if (condition1 && condition2 && condition3 && condition4) {
                return true
            }
        }
        return false
    }
}
