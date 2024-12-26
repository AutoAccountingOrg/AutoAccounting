/*
 * Copyright (C) 2024 ankio(ankio@ankio.net)
 * Licensed under the Apache License, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-3.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package net.ankio.dex

import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.dexlib2.dexbacked.DexBackedMethod
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
import java.lang.reflect.Method

fun DexBackedClassDef.getStrings(): List<String> {
    val strings = mutableListOf<String>()
    
    // 遍历所有方法
    methods.forEach { method ->
        strings.addAll(method.getStrings())
    }
    
    return strings.distinct()
}

fun DexBackedMethod.getStrings(): List<String> {
    val strings = mutableListOf<String>()

    // 获取方法的代码
    val methodImpl = this.implementation
    methodImpl?.instructions?.forEach { instruction ->
        // 检查是否是字符串类型的指令
        if (instruction.opcode.name.contains("const-string")) {
            // 使用 ReferenceInstruction 接口获取字符串引用
            if (instruction is ReferenceInstruction) {
                val reference = instruction.reference
                if (reference is StringReference) {
                    val stringValue = reference.string
                    strings.add(stringValue)
                }
            }
        }
    }

    return strings.distinct()
}

fun DexBackedClassDef.getMethodStrings(method: Method): List<String> {
    // 遍历所有方法
    val list = mutableListOf<String>()
    this.methods.forEach { m ->
        if (m.name == method.name) {
          list.addAll(m.getStrings())
        }
    }
    return list
}

