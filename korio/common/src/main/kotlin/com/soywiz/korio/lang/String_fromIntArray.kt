package com.soywiz.korio.lang

fun String_fromIntArray(arrays: IntArray, offset: Int = 0, size: Int = arrays.size - offset): String {
	val sb = StringBuilder()
	for (n in offset until offset + size) {
		sb.append(arrays[n].toChar()) // @TODO: May not work the same! In JS: String.fromCodePoint
	}
	return sb.toString()
}

fun String_fromCharArray(arrays: CharArray, offset: Int = 0, size: Int = arrays.size - offset): String {
	val sb = StringBuilder()
	for (n in offset until offset + size) {
		sb.append(arrays[n].toChar()) // @TODO: May not work the same! In JS: String.fromCodePoint
	}
	return sb.toString()
}