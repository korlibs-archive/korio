package com.soywiz.korio.util

import com.soywiz.korio.ds.ByteArrayBuilder
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toByteArray

fun String.toBytez(len: Int, charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayBuilder()
	out.append(this.toByteArray(charset))
	while (out.size < len) out.append(0.toByte())
	return out.toByteArray()
}

fun String.toBytez(charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayBuilder()
	out.append(this.toByteArray(charset))
	out.append(0.toByte())
	return out.toByteArray()
}

fun String.indexOfOrNull(char: Char, startIndex: Int = 0): Int? {
	val i = this.indexOf(char, startIndex)
	return if (i >= 0) i else null
}

fun String.lastIndexOfOrNull(char: Char, startIndex: Int = lastIndex): Int? {
	val i = this.lastIndexOf(char, startIndex)
	return if (i >= 0) i else null
}

fun String.splitInChunks(size: Int): List<String> {
	val out = arrayListOf<String>()
	var pos = 0
	while (pos < this.length) {
		out += this.substring(pos, kotlin.math.min(this.length, pos + size))
		pos += size
	}
	return out
}

fun String.substr(start: Int): String = this.substr(start, this.length)

fun String.substr(start: Int, length: Int): String {
	val low = (if (start >= 0) start else this.length + start).clamp(0, this.length)
	val high = (if (length >= 0) low + length else this.length + length).clamp(0, this.length)
	if (high < low) {
		return ""
	} else {
		return this.substring(low, high)
	}
}

fun String.transform(transform: (Char) -> String): String {
	var out = ""
	for (ch in this) out += transform(ch)
	return out
}

fun Any?.toBetterString(): String {
	/*
	if (this == null) return "null"
	val clazz = this::class.java
	if (clazz.isArray) return "" + ReflectedArray(this).toList()
	return "$this"
	*/
	return "$this"
}
