package com.soywiz.korio.util

import com.soywiz.korio.crypto.Hex

operator fun ByteArray.set(o: Int, v: Int) = run { this[o] = v.toByte() }
operator fun ByteArray.set(o: Int, v: Long) = run { this[o] = v.toByte() }

fun ByteArray.getu(o: Int) = this[o].toUnsigned()

fun List<ByteArray>.join(): ByteArray {
	val out = ByteArray(this.sumBy { it.size })
	var pos = 0
	for (c in this) {
		System.arraycopy(c, 0, out, pos, c.size)
		pos += c.size
	}
	return out
}

val ByteArray.hexString: String get() = Hex.encodeLower(this)

fun ByteArray.toHexString() = Hex.encode(this)
fun ByteArray.toHexStringLower() = Hex.encodeLower(this)
fun ByteArray.toHexStringUpper() = Hex.encodeUpper(this)

fun ByteArray.indexOfElse(element: Byte, default: Int = this.size): Int {
	val idx = this.indexOf(element)
	return if (idx >= 0) idx else default
}

fun ByteArray.indexOf(startOffset: Int, v: Byte): Int {
	for (n in startOffset until this.size) if (this[n] == v) return n
	return -1
}

fun String.fromHexString(): ByteArray = Hex.decode(this)