package com.soywiz.korio.util

import com.soywiz.kmem.*

operator fun ByteArray.set(o: Int, v: Int) = run { this[o] = v.toByte() }
operator fun ByteArray.set(o: Int, v: Long) = run { this[o] = v.toByte() }

fun ByteArray.getu(o: Int) = this[o].toUnsigned()

fun List<ByteArray>.join(): ByteArray {
	val out = ByteArray(this.sumBy { it.size })
	var pos = 0
	for (c in this) {
		arraycopy(c, 0, out, pos, c.size)
		pos += c.size
	}
	return out
}

fun ByteArray.indexOfElse(element: Byte, default: Int = this.size): Int {
	val idx = this.indexOf(element)
	return if (idx >= 0) idx else default
}

fun ByteArray.indexOf(startOffset: Int, v: Byte): Int {
	for (n in startOffset until this.size) if (this[n] == v) return n
	return -1
}
