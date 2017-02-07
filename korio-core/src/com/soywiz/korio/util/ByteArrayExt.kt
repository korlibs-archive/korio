package com.soywiz.korio.util

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

val HEX_DIGITS = "0123456789ABCDEF"
val HEX_DIGITS_UPPER = HEX_DIGITS.toUpperCase()
val HEX_DIGITS_LOWER = HEX_DIGITS.toLowerCase()

private fun toHexStringBase(data: ByteArray, digits: String = HEX_DIGITS): String {
	val out = CharArray(data.size * 2)
	var m = 0
	for (n in data.indices) {
		val v = data[n].toInt() and 0xFF
		out[m++] = digits[(v ushr 4) and 0xF]
		out[m++] = digits[(v ushr 0) and 0xF]
	}
	return String(out)
}

fun ByteArray.toHexString() = toHexStringBase(this, HEX_DIGITS_UPPER)
fun ByteArray.toHexStringLower() = toHexStringBase(this, HEX_DIGITS_LOWER)
fun ByteArray.toHexStringUpper() = toHexStringBase(this, HEX_DIGITS_UPPER)

fun ByteArray.indexOfElse(element: Byte, default: Int = this.size): Int {
	val idx = this.indexOf(element)
	return if (idx >= 0) idx else default
}

fun ByteArray.indexOf(startOffset: Int, v: Byte): Int {
	for (n in startOffset until this.size) if (this[n] == v) return n
	return -1
}