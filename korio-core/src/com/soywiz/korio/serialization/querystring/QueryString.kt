package com.soywiz.korio.serialization.querystring

import java.net.URLDecoder
import java.net.URLEncoder

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