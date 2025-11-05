package com.wynnscribe.utils

private val regexLiterals = Regex("[.*+?^\${}()|\\[\\]\\\\]")

fun escapeRegex(str: String): String {
    return str.replace(regexLiterals) { "\\${it.value}" }
}

private val groupingRegex = Regex("(?<!\\\\)\\((?!\\?)")

fun escapeGroupingRegex(str: String): String {
    return str.replace(groupingRegex, "(?:")
}