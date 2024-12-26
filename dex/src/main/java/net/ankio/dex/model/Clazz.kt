package net.ankio.dex.model

data class Clazz(
    val name: String = "",
    val fields: List<ClazzField> = listOf(),
    val methods: List<ClazzMethod> = listOf(),
    val nameRule: String = "",
    val type: String = "",
    val strings: List<String> = listOf(),
)
