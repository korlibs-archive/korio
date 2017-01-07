package com.soywiz.korio.util

fun List<ByteArray>.join(): ByteArray {
	val out = ByteArray(this.sumBy { it.size })
	var pos = 0
	for (c in this) {
		System.arraycopy(c, 0, out, pos, c.size)
		pos += c.size
	}
	return out
}