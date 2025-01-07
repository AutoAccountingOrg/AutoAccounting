/*
 * Copyright (C) 2025 ankio(ankio@ankio.net)
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

package net.ankio.auto.xposed.hooks.qianji.models

import de.robv.android.xposed.XposedHelpers
import net.ankio.auto.xposed.core.hook.Hooker

class Category(private val categoryObj: Any) {
    companion object {
        const val EDITABLE_NOT = 0
        const val EDITABLE_YES = 1
        const val LEVEL_1 = 1
        const val LEVEL_2 = 2
        const val NON_PARENT_ID = -1
        val categoryClazz = Hooker.loader("com.mutangtech.qianji.data.model.Category")

        fun fromObject(obj: Any): Category = Category(obj)

        /**
         * 检查是否支持账单类型
         */
        fun supportByBillType(type: Int): Boolean {
            return XposedHelpers.callStaticMethod(
                categoryClazz,
                "supportByBillType",
                type
            ) as Boolean
        }
    }

    fun toObject(): Any = categoryObj

    /**
     * 添加子分类
     */
    fun addSubCategory(category: Category?, needSort: Boolean): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "addSubCategory",
            category?.toObject(),
            needSort
        ) as Boolean
    }

    /**
     * 从其他分类复制
     */
    fun copyFrom(category: Category) {
        XposedHelpers.callMethod(
            categoryObj,
            "copyFrom",
            category.toObject()
        )
    }

    /**
     * 获取账本ID
     */
    fun getBookId(): Long {
        return XposedHelpers.callMethod(
            categoryObj,
            "getBookId"
        ) as Long
    }

    /**
     * 获取可编辑状态
     */
    fun getEditable(): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "getEditable"
        ) as Int
    }

    /**
     * 获取图标
     */
    fun getIcon(): String? {
        return XposedHelpers.callMethod(
            categoryObj,
            "getIcon"
        ) as String?
    }

    /**
     * 获取图标文本
     */
    fun getIconText(): String? {
        return XposedHelpers.callMethod(
            categoryObj,
            "getIconText"
        ) as String?
    }

    /**
     * 获取ID
     */
    fun getId(): Long {
        return XposedHelpers.callMethod(
            categoryObj,
            "getId"
        ) as Long
    }

    /**
     * 获取层级
     */
    fun getLevel(): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "getLevel"
        ) as Int
    }

    /**
     * 获取名称
     */
    fun getName(): String? {
        return XposedHelpers.callMethod(
            categoryObj,
            "getName"
        ) as String?
    }

    /**
     * 获取父分类ID
     */
    fun getParentId(): Long {
        return XposedHelpers.callMethod(
            categoryObj,
            "getParentId"
        ) as Long
    }

    /**
     * 获取排序值
     */
    fun getSort(): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "getSort"
        ) as Int
    }

    /**
     * 获取子分类列表
     */
    @Suppress("UNCHECKED_CAST")
    fun getSubList(): List<Category>? {
        return XposedHelpers.callMethod(categoryObj, "getSubList")?.let { list ->
            (list as List<Any>).map { fromObject(it) }
        }
    }

    /**
     * 获取类型
     */
    fun getType(): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "getType"
        ) as Int
    }

    /**
     * 获取用户ID
     */
    fun getUserId(): String? {
        return XposedHelpers.callMethod(
            categoryObj,
            "getUserId"
        ) as String?
    }

    /**
     * 检查是否有子分类
     */
    fun hasSubList(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "hasSubList"
        ) as Boolean
    }

    /**
     * 检查是否可编辑
     */
    fun isEditable(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "isEditable"
        ) as Boolean
    }

    /**
     * 检查是否为收入
     */
    fun isIncome(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "isIncome"
        ) as Boolean
    }

    /**
     * 检查是否为父分类
     */
    fun isParentCategory(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "isParentCategory"
        ) as Boolean
    }

    /**
     * 检查是否为支出
     */
    fun isSpend(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "isSpend"
        ) as Boolean
    }

    /**
     * 检查是否为子分类
     */
    fun isSubCategory(): Boolean {
        return XposedHelpers.callMethod(
            categoryObj,
            "isSubCategory"
        ) as Boolean
    }

    /**
     * 移除子分类
     */
    fun removeSubCategory(category: Category): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "removeSubCategory",
            category.toObject()
        ) as Int
    }

    /**
     * 设置账本ID
     */
    fun setBookId(id: Long) {
        XposedHelpers.callMethod(
            categoryObj,
            "setBookId",
            id
        )
    }

    /**
     * 设置可编辑状态
     */
    fun setEditable(editable: Int) {
        XposedHelpers.callMethod(
            categoryObj,
            "setEditable",
            editable
        )
    }

    /**
     * 设置图标
     */
    fun setIcon(icon: String?) {
        XposedHelpers.callMethod(
            categoryObj,
            "setIcon",
            icon
        )
    }

    /**
     * 设置ID
     */
    fun setId(id: Long) {
        XposedHelpers.callMethod(
            categoryObj,
            "setId",
            id
        )
    }

    /**
     * 设置层级
     */
    fun setLevel(level: Int) {
        XposedHelpers.callMethod(
            categoryObj,
            "setLevel",
            level
        )
    }

    /**
     * 设置名称
     */
    fun setName(name: String?) {
        XposedHelpers.callMethod(
            categoryObj,
            "setName",
            name
        )
    }

    /**
     * 设置父分类ID
     */
    fun setParentId(id: Long) {
        XposedHelpers.callMethod(
            categoryObj,
            "setParentId",
            id
        )
    }

    /**
     * 设置排序值
     */
    fun setSort(sort: Int) {
        XposedHelpers.callMethod(
            categoryObj,
            "setSort",
            sort
        )
    }

    /**
     * 设置类型
     */
    fun setType(type: Int) {
        XposedHelpers.callMethod(
            categoryObj,
            "setType",
            type
        )
    }

    /**
     * 设置用户ID
     */
    fun setUserId(userId: String?) {
        XposedHelpers.callMethod(
            categoryObj,
            "setUserId",
            userId
        )
    }

    /**
     * 对子分类列表排序
     */
    fun sortSubList() {
        XposedHelpers.callMethod(
            categoryObj,
            "sortSubList"
        )
    }

    /**
     * 获取子分类数量
     */
    fun subListCount(): Int {
        return XposedHelpers.callMethod(
            categoryObj,
            "subListCount"
        ) as Int
    }
}