package com.soywiz.korio.serialization.querystring

import java.net.URLDecoder

object QueryString {
	fun decode(str: CharSequence): Map<String, List<String>> {
		val out = LinkedHashMap<String, ArrayList<String>>()
		for (chunk in str.split('&')) {
			val parts = chunk.split('=', limit = 2)
			val key = URLDecoder.decode(parts[0], "UTF-8")
			val value = URLDecoder.decode(parts.getOrElse(1) { key }, "UTF-8")
			val list = out.getOrPut(key) { arrayListOf() }
			list += value
		}
		return out
	}
}