package com.soywiz.korio.util

fun Regex.Companion.quote(str: String): String = str.replace(Regex("[.?*+^\$\\[\\]\\\\(){}|\\-]")) { "\\${it.value}" }
