package com.soywiz.korio.net

import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

object URLDecoder {
	fun decode(s: String, enc: String): String {
		val bos = ByteArrayBuilder()
		val len = s.length
		var n = 0
		while (n < len) {
			val c = s[n]
			when (c) {
				'%' -> {
					bos.append(s.substr(n + 1, 2).toInt(16).toByte())
					n += 2
				}
				'+' -> bos.append(' '.toInt().toByte())
				else -> bos.append(c.toByte())
			}
			n++
		}
		return bos.toByteArray().toString(Charset.forName(enc))

	}
}
