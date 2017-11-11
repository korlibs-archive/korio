package com.soywiz.korio.util

import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString

fun ByteArray.readStringz(o: Int, size: Int, charset: Charset = UTF8): String {
	var idx = o
	val stop = kotlin.math.min(this.size, o + size)
	while (idx < stop) {
		if (this[idx] == 0.toByte()) break
		idx++
	}
	return this.copyOfRange(o, idx).toString(charset)
}

fun ByteArray.readStringz(o: Int, charset: Charset = UTF8): String {
	return readStringz(o, size - o, charset)
}

fun ByteArray.readString(o: Int, size: Int, charset: Charset = UTF8): String {
	return this.copyOfRange(o, o + size).toString(charset)
}