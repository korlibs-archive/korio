package com.soywiz.korio.lang

import java.lang.Float

class Float16(val bits: Int) {
	constructor(value: Double) : this(doubleToIntBits(value))

	val value: Double by lazy { intBitsToDouble(bits) }

	companion object {
		const val FLOAT16_EXPONENT_BASE = 15

		fun intBitsToDouble(word: Int): Double {
			val sign = if ((word and 0x8000) != 0) -1 else 1
			val exponent = (word ushr 10) and 0x1f
			val significand = word and 0x3ff
			if (exponent == 0) {
				if (significand == 0) {
					return 0.0
				} else {
					// subnormal number
					return sign * Math.pow(2.0, (1 - FLOAT16_EXPONENT_BASE).toDouble()) * (significand / 1024)
				}
			}
			if (exponent == 31) {
				if (significand == 0) {
					return if (sign < 0) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
				} else {
					return Double.NaN
				}
			}
			// normal number
			return sign * Math.pow(2.0, (exponent - FLOAT16_EXPONENT_BASE).toDouble()) * (1 + significand / 1024)
		}

		fun doubleToIntBits(value: Double): Int {
			val dword = Float.floatToIntBits(value.toFloat())

			return if ((dword and 0x7FFFFFFF) == 0) {
				dword ushr 16
			} else {
				val sign = dword and 0x80000000.toInt()
				val exponent = dword and 0x7FF00000
				var significand = dword and 0x000FFFFF

				if (exponent == 0) {
					sign ushr 16
				} else if (exponent == 0x7FF00000) {
					if (significand == 0) ((sign ushr 16) or 0x7C00) else 0xFE00
				} else {
					val signedExponent = (exponent ushr 20) - 1023 + 15
					var halfSignificand = 0
					if (signedExponent >= 0x1F) {
						((significand ushr 16) or 0x7C00)
					} else if (signedExponent <= 0) {
						if ((10 - signedExponent) > 21) {
							halfSignificand = 0
						} else {
							significand = significand or 0x00100000
							halfSignificand = (significand ushr (11 - signedExponent))
							if (((significand ushr (10 - signedExponent)) and 0x00000001) != 0) halfSignificand += 1
						}
						((sign ushr 16) or halfSignificand)
					} else {
						halfSignificand = significand ushr 10
						val out = (sign or (signedExponent shl 10) or halfSignificand)
						if ((significand and 0x00000200) != 0) out + 1 else out
					}
				}
			}
		}
	}
}
