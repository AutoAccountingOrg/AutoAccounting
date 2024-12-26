package net.ankio.dex

import net.ankio.dex.model.Clazz
import net.ankio.dex.model.ClazzField
import net.ankio.dex.model.ClazzMethod
import net.ankio.dex.result.ClazzResult
import net.ankio.dex.result.FieldResult
import net.ankio.dex.result.MethodResult
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import java.io.BufferedInputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.zip.ZipFile

object Dex {

    var DEBUG = true

    fun print(msg: String) {
        if (DEBUG) {
            println("[DEX] $msg")
        }
    }

    private fun getAllDexFiles(file: String): List<DexBackedDexFile> {
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
    ): HashMap<String, ClazzResult> {
        val results = HashMap<String, ClazzResult>()

        getAllDexFiles(path).forEach dexPath@{ dexFile ->
            if (results.size == rules.size) {
                //print("All classes found")
                return@dexPath  // 所有需要查找的元素全部找到
            }
            dexFile.classes.forEach dex@{ classDef ->
                if (results.size == rules.size) {
                    //print("All classes found")
                    return@dexPath
                }// 所有需要查找的元素全部找到


                val className =
                    classDef.type.substring(1, classDef.type.length - 1).replace('/', '.')
               // //print("Class : $className")

                rules.forEach rules@{ clazzRule ->
                    if (results.containsKey(clazzRule.name)) {
                        //print("Class ${clazzRule.name} already found")
                        return@rules  // 已找到此类，跳过
                    }

                    // 判断包名规则有要求吗？有要求就使用正则匹配
                    val matchNameRule =
                        clazzRule.nameRule.isEmpty() || Regex(clazzRule.nameRule).matches(className)
                    if (!matchNameRule) {
                       // //print("ClassName not match ${clazzRule.nameRule} ")
                        return@rules
                    } // 包名不匹配，跳过


                    // 尝试加载类
                    val clazz = runCatching { classLoader.loadClass(className) }.onFailure {
                        //print("Class $className load failed. ")
                    }.getOrNull()
                        ?: return@rules
                    //print("Class $className load success. ")
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
                            results[clazzRule.name] = ClazzResult(clazz.name)
                            //print("Class $className found success. ")
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

                    //print("Class $className Fields is match: $matchFields. ")

                    if (clazzRule.strings.isNotEmpty()) {
                        val strings = classDef.getStrings()
                        val matchStrings = clazzRule.strings.all { string ->
                            strings.any { it == string }
                        }
                        if (!matchStrings) {
                            return@rules
                        } // 字符串不匹配，跳过
                    }

                    val methods = HashMap<String,MethodResult>()
                    // 检查方法
                    val matchMethods =
                        clazzRule.methods.isEmpty() || clazzRule.methods.all { method ->
                            val m = findMethodIsExist(clazz, method, classDef)
                            if (m != null) {
                                if (method.findName.isNotEmpty()){
                                    methods[method.findName] = m
                                }
                                true
                            } else {
                                false
                            }
                        }
                    if (!matchMethods) {
                        return@rules
                    } // 方法不匹配，跳过
                    results[clazzRule.name] = ClazzResult(clazz.name, methods)
                }
            }
        }

        //print("Found classes: $results")

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

                //print("Field  match ${f.name} == ${field.name}. ")

                // 检查字段类型
                val matchType = field.type.isEmpty() || f.type.name == field.type


                //print("Field Type  match ${f.type.name} == ${field.type}. ")

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
        clazzMethod: ClazzMethod,
        classDef: DexBackedClassDef
    ): MethodResult? {
        if (with(clazzMethod) { name.isEmpty() && returnType.isEmpty() && modifiers.isEmpty() && parameters.isEmpty() }) {
            return null
        }

        return runCatching {
            if (clazzMethod.name == "constructor") {
                 clazz.declaredConstructors.forEach { method ->
                    if (method.parameters.size == clazzMethod.parameters.size &&
                        method.parameters.mapIndexed { index, param ->
                            param.type.name == clazzMethod.parameters[index].type
                        }.all { it }){
                        return MethodResult(method.name)
                    }
                }
                return null
            }
            clazz.declaredMethods.forEach { method ->
                val matchName = clazzMethod.name.isEmpty() || method.name == clazzMethod.name
                if (!matchName) return@forEach
                val matchReturnType =
                    clazzMethod.returnType.isEmpty() || method.returnType.name == clazzMethod.returnType
                if (!matchReturnType) return@forEach
                // //print("Method matchReturnType ${clazz.name}.${method.returnType.name} => ${clazzMethod.returnType} ")
                val matchModifiers =
                    clazzMethod.modifiers.isEmpty() || method.modifiers.toString() == clazzMethod.modifiers
                if (!matchModifiers) return@forEach
                //  //print("Method matchModifiers ${clazz.name}.${method.modifiers} => ${clazzMethod.modifiers} ")
                val matchParameters = clazzMethod.parameters.isEmpty() || (
                        method.parameters.size == clazzMethod.parameters.size &&
                                method.parameters.mapIndexed { index, param ->
                                    //   if (
                                    param.type.name == clazzMethod.parameters[index].type
                                    //)
                                    //       //print("Method matchParameters ${clazz.name}.${clazzMethod.parameters} => ${clazzMethod.parameters}")
                                    //   false
                                }.all { it }
                        )



                if (!matchParameters) return@forEach

                if (clazzMethod.strings.isNotEmpty()) {
                    val strings = classDef.getMethodStrings(method)
                    //  println("Method strings ${clazz.name}.${method.name} strings: $strings")
                    val matchString =clazzMethod.strings.all { string ->
                        strings.any { it == string }
                    }
                    if (!matchString) return@forEach
                }
                return MethodResult(method.name)
            }

            null

        }.getOrElse { null }  // 如果反射调用失败，返回 false
    }


}
