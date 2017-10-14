package com.soywiz.korio.crypto

import com.soywiz.korio.util.substr

object Hex {
	val DIGITS = "0123456789ABCDEF"
	val DIGITS_UPPER = DIGITS.toUpperCase()
	val DIGITS_LOWER = DIGITS.toLowerCase()

	fun isHexDigit(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

	fun decode(str: String): ByteArray {
		val out = ByteArray(str.length / 2)
		for (n in 0 until out.size) {
			out[n] = (str.substr(n * 2, 2).toIntOrNull(16) ?: 0).toByte()
		}
		return out
	}

	fun encode(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)

	fun encodeLower(src: ByteArray): String = encodeBase(src, DIGITS_LOWER)
	fun encodeUpper(src: ByteArray): String = encodeBase(src, DIGITS_UPPER)

	private fun encodeBase(data: ByteArray, digits: String = DIGITS): String {
		val out = StringBuilder(data.size * 2)
		for (n in data.indices) {
			val v = data[n].toInt() and 0xFF
			out.append(digits[(v ushr 4) and 0xF])
			out.append(digits[(v ushr 0) and 0xF])
		}
		return out.toString()
	}
}