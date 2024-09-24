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
        rules: List<Clazz>
    ): HashMap<String, String> {
        val results = HashMap<String, String>()

        getAllDexFiles(path).forEach dexPath@{ dexFile ->
            if (results.size == rules.size) {
             //   println("[DEX] All classes found")
                return@dexPath  // 所有需要查找的元素全部找到
            }
            dexFile.classes.forEach dex@{ classDef ->
                if (results.size == rules.size) {
                 //   println("[DEX] All classes found")
                    return@dexPath
                }// 所有需要查找的元素全部找到

                val className =
                    classDef.type.substring(1, classDef.type.length - 1).replace('/', '.')
           //     println("[DEX] Class : $className")

                rules.forEach rules@{ clazzRule ->
                    if (results.containsKey(clazzRule.name)) {
                  //      println("[DEX] Class ${clazzRule.name} already found")
                        return@rules  // 已找到此类，跳过
                   }

                    // 判断包名规则有要求吗？有要求就使用正则匹配
                    val matchNameRule =
                        clazzRule.nameRule.isEmpty() || Regex(clazzRule.nameRule).matches(className)
                    if (!matchNameRule) {
                   //     println("[DEX] ClassName not match ${clazzRule.nameRule} ")
                        return@rules
                    } // 包名不匹配，跳过

                    // 尝试加载类
                    val clazz = runCatching { classLoader.loadClass(className) }.getOrNull()
                        ?: return@rules
                    //println("[DEX] Class $className load success. ")
                    // 判断类的类型条件
                    val matchType = clazzRule.type.isEmpty() || when (clazzRule.type) {
                        "interface" -> clazz.isInterface
                        "abstract" -> Modifier.isAbstract(clazz.modifiers)
                        "enum" -> clazz.isEnum
                        else -> true
                    }

                    if (!matchType) {
                        return@rules
                    } // 类型不匹配，跳过



                    // 处理枚举类型的匹配
                    if (clazzRule.type == "enum" && clazz.isEnum) {
                        val matchEnumFields = clazzRule.fields.all { field ->
                            clazz.enumConstants.any { enumConstant ->
                                (enumConstant as Enum<*>).name == field.name
                            }
                        }
                        if (matchEnumFields) {
                            results[clazzRule.name] = className
                            println("[DEX] Class $className found success. ")
                            return@rules  // 匹配成功，跳过其他规则
                        }
                    }

                    // 检查字段
                    val matchFields = clazzRule.fields.isEmpty() || clazzRule.fields.all { field ->
                        findFieldIsExist(clazz, field)
                    }

                    if (!matchFields) {
                        return@rules
                    } // 字段不匹配，跳过

               //     println("[DEX] Class $className Fields is match: $matchFields. ")

                    // 检查方法
                    val matchMethods =
                        clazzRule.methods.isEmpty() || clazzRule.methods.all { method ->
                            findMethodIsExist(clazz, method)
                        }
                    if (!matchMethods) {
                        return@rules
                    } // 方法不匹配，跳过
                    results[clazzRule.name] = className
                }
            }
        }

        println("[DEX] Found classes: $results")

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
        field: ClazzField
    ): Boolean {
        // 如果字段名和类型都是空的，直接返回 false
        if (field.name.isEmpty() && field.type.isEmpty()) {
            return false
        }

        return runCatching {
            clazz.declaredFields.any { f ->
                f.isAccessible = true

                // 检查字段名
                val matchName = field.name.isEmpty() || f.name == field.name

          //      println("[DEX] Field  match ${f.name} == ${field.name}. ")

                // 检查字段类型
                val matchType = field.type.isEmpty() || f.type.name == field.type


             //   println("[DEX] Field Type  match ${f.type.name} == ${field.type}. ")

                // 所有条件都满足则返回 true
                matchName && matchType
            }
        }.getOrElse { false }  // 如果出现异常，返回 false
    }


    /**
     * 查找某个方法
     */
    fun findMethod(
        clazz: Class<*>,
        clazzMethod: ClazzMethod
    ): String {
        return runCatching {
            clazz.declaredMethods.firstOrNull { method ->
                val matchName =
                    clazzMethod.regex.isEmpty() || Regex(clazzMethod.regex).matches(method.name)
                val matchReturnType =
                    clazzMethod.returnType.isEmpty() || method.returnType.name == clazzMethod.returnType
                val matchModifiers =
                    clazzMethod.modifiers.isEmpty() || method.modifiers.toString() == clazzMethod.modifiers
                val matchParameters = clazzMethod.parameters.isEmpty() || (
                        method.parameters.size == clazzMethod.parameters.size &&
                                method.parameters.mapIndexed { index, param ->
                                    param.type.name == clazzMethod.parameters[index].type
                                }.all { it }
                        )

                matchName && matchReturnType && matchModifiers && matchParameters
            }?.name.orEmpty()


        }.getOrElse { "" }  // 如果反射调用失败，返回空字符串
    }


    /**
     * 判断某个方法是否在给定的clazz里面
     */
    private fun findMethodIsExist(
        clazz: Class<*>,
        clazzMethod: ClazzMethod
    ): Boolean {
        if (with(clazzMethod) { name.isEmpty() && returnType.isEmpty() && modifiers.isEmpty() && parameters.isEmpty() }) {
            return false
        }

        return runCatching {
            if ( clazzMethod.name == "constructor") {
                return clazz.declaredConstructors.any { method ->
                    method.parameters.size == clazzMethod.parameters.size &&
                            method.parameters.mapIndexed { index, param ->
                                param.type.name == clazzMethod.parameters[index].type
                            }.all { it }
                }
            }
           clazz.methods.any { method ->
                val matchName = clazzMethod.name.isEmpty() || method.name == clazzMethod.name
                if (!matchName) return@any false
              //  println("[DEX] Method Name ${clazz.name}.${method.name} => ${clazzMethod.name} ")
                val matchReturnType = clazzMethod.returnType.isEmpty() || method.returnType.name == clazzMethod.returnType
                if (!matchReturnType) return@any false
              //  println("[DEX] Method matchReturnType ${clazz.name}.${method.returnType.name} => ${clazzMethod.returnType} ")
                val matchModifiers = clazzMethod.modifiers.isEmpty() || method.modifiers.toString() == clazzMethod.modifiers
                if (!matchModifiers) return@any false
             //   println("[DEX] Method matchModifiers ${clazz.name}.${method.modifiers} => ${clazzMethod.modifiers} ")
                val matchParameters = clazzMethod.parameters.isEmpty() || (
                        method.parameters.size == clazzMethod.parameters.size &&
                                method.parameters.mapIndexed { index, param ->
                                 //   if (
                                        param.type.name == clazzMethod.parameters[index].type
                        //)
                             //       println("[DEX] Method matchParameters ${clazz.name}.${clazzMethod.parameters} => ${clazzMethod.parameters}")
                                 //   false
                                }.all { it }
                        )



                if (!matchParameters) return@any false

                true
            }
        }.getOrElse { false }  // 如果反射调用失败，返回 false
    }


}
