package net.ankio.auto.xposed.hooks.qianji.impl

/**
 * 按钱迹 SearchPresenterImpl.searchLocal 的实际参数个数组装调用参数。
 * 抽成纯函数便于单测，避免依赖 Xposed / ClassLoader。
 *
 * 已知签名演进：
 * - ≤12：到 BillFlagFilter 为止
 * - +AssetsFilter / +TagsFilter / +CategoriesFilter
 * - 钱迹 4.5.4：在 CategoriesFilter 后再跟两个 boolean
 *   （includeNotCount / includeNotBudget，搜索页默认均为 true）
 */
internal object SearchLocalArgs {
    fun build(
        paramCount: Int,
        bookFilter: Any,
        dateFilter: Any,
        typesFilter: Any,
        sortFilter: Any,
        billFlagFilter: Any,
        search: String = "",
        moneyFilter: Any? = null,
        imageFilter: Any? = null,
        platformFilter: Any? = null,
        assetsFilter: Any? = null,
        tagsFilter: Any? = null,
        categoriesFilter: Any? = null,
        z10: Boolean = false,
        z11: Boolean = false,
        z12: Boolean = true,
        includeNotCount: Boolean = true,
        includeNotBudget: Boolean = true,
    ): Array<Any?> {
        require(paramCount >= 12) {
            "searchLocal method must have at least 12 parameters, was $paramCount"
        }
        val params = ArrayList<Any?>(paramCount)
        params += search
        params += bookFilter
        params += dateFilter
        params += typesFilter
        params += moneyFilter
        params += imageFilter
        params += platformFilter
        params += z10
        params += z11
        params += z12
        params += sortFilter
        params += billFlagFilter
        if (paramCount > 12) params += assetsFilter
        if (paramCount > 13) params += tagsFilter
        if (paramCount > 14) params += categoriesFilter
        if (paramCount > 15) params += includeNotCount
        if (paramCount > 16) params += includeNotBudget
        // 若未来再追加参数，用 null 填齐，避免 findMethodBestMatch 因参数个数失败
        while (params.size < paramCount) {
            val idx = params.size
            // 布尔位默认 true；其余默认 null
            params += if (idx >= 15) true else null
        }
        return params.toTypedArray()
    }
}
