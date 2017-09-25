package com.soywiz.korio.serialization.querystring

import com.soywiz.korio.ds.ByteArrayBuilder
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.lang.toString
import com.soywiz.korio.util.substr

object QueryString {
	fun decode(str: CharSequence): Map<String, List<String>> {
		val out = lmapOf<String, ArrayList<String>>()
		for (chunk in str.split('&')) {
			val parts = chunk.split('=', limit = 2)
			val key = URLDecoder.decode(parts[0], "UTF-8")
			val value = URLDecoder.decode(parts.getOrElse(1) { key }, "UTF-8")
			val list = out.getOrPut(key) { arrayListOf() }
			list += value
		}
		return out
	}

	fun encode(map: Map<String, List<String>>): String {
		return encode(*map.flatMap { (key, value) -> value.map { key to it } }.toTypedArray())
	}

	fun encode(vararg items: Pair<String, String>): String {
		val parts = arrayListOf<String>()
		for ((key, value) in items) {
			parts += URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8")
		}
		return parts.joinToString("&")
	}
}

object URLDecoder {
	fun decode(s: String, enc: String): String {
		val bos = ByteArrayBuilder()
		val len = s.length
		var n = 0
		while (n < len) {
			val c = s[n]
			if (c == '%') {
				bos.append(s.substr(n + 1, 2).toInt(16).toByte())
				n += 2
			} else if (c == '+') {
				bos.append(' '.toInt().toByte())
			} else {
				bos.append(c.toByte())
			}
			n++
		}
		return bos.toByteArray().toString(Charset.forName(enc))

	}
}

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
			if (c == ' '.toByte()) {
				sb.append('+')
			} else if (normal.get(c.toInt() and 0xFF)) {
				sb.append(c.toChar())
			} else {
				sb.append('%')
				sb.append(digits[c.toInt().ushr(4) and 0xF])
				sb.append(digits[c.toInt().ushr(0) and 0xF])
			}
		}
		return sb.toString()
	}
}