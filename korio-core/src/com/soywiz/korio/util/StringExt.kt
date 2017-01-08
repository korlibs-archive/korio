package com.soywiz.korio.util

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

fun String.toBytez(len: Int, charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayOutputStream()
	out.write(this.toByteArray(charset))
	while (out.size() < len) out.write(0)
	return out.toByteArray()
}

fun String.toBytez(charset: Charset = Charsets.UTF_8): ByteArray {
	val out = ByteArrayOutputStream()
	out.write(this.toByteArray(charset))
	out.write(0)
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
		out += this.substring(pos, Math.min(this.length, pos + size))
		pos += size
	}
	return out
}