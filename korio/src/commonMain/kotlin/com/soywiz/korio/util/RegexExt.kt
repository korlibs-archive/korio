package com.soywiz.korio.util

//fun Regex.Companion.quote(str: String): String = str.replace(Regex("[.?*+^\$\\[\\]\\\\(){}|\\-]")) { "\\${it.value}" }

fun Regex.Companion.quote(str: String): String = buildString {
	for (c in str) {
		when (c) {
			'.', '?', '*', '+', '^', '\\', '$', '[', ']', '(', ')', '{', '}', '|', '-' -> append('\\')
		}
		append(c)
	}
}