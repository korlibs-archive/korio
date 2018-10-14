package com.soywiz.korio.net

import com.soywiz.korio.lang.*

object URLEncoder {
	private val normal = BooleanArray(0x100)
	private val normalTable = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 -_.*"
	private val digits = "0123456789ABCDEF"

	init {
		for (c in normalTable) normal[c.toInt()] = true
	}

	fun encode(s: String, enc: String): String {
		val sb = StringBuilder(s.length)
		val data = s.toByteArray(Charset.forName(enc))
		//for (byte c : data) System.out.printf("%02X\n", c & 0xFF);
		for (c in data) {
			when {
				c == ' '.toByte() -> sb.append('+')
				normal[c.toInt() and 0xFF] -> sb.append(c.toChar())
				else -> {
					sb.append('%')
					sb.append(digits[c.toInt().ushr(4) and 0xF])
					sb.append(digits[c.toInt().ushr(0) and 0xF])
				}
			}
		}
		return sb.toString()
	}
}
