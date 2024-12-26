package net.ankio.dex.model

data class ClazzMethod(
    val name: String = "",
    val returnType: String = "",
    val modifiers: String = "",
    val parameters: List<ClazzField> = listOf(),
    val regex: String = "",
    val strings: List<String> = listOf(),
    val findName: String = "",
)
