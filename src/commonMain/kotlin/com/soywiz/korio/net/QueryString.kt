package com.soywiz.korio.net

import com.soywiz.korio.lang.*

object QueryString {
	fun decode(str: CharSequence): Map<String, List<String>> {
		val out = linkedMapOf<String, ArrayList<String>>()
		for (chunk in str.split('&')) {
			val parts = chunk.split('=', limit = 2)
			val key = URL.decodeComponent(parts[0], UTF8, formUrlEncoded = true)
			val value = URL.decodeComponent(parts.getOrElse(1) { key }, UTF8, formUrlEncoded = true)
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
			parts += URL.encodeComponent(key, UTF8, formUrlEncoded = true) + "=" + URL.encodeComponent(value, UTF8, formUrlEncoded = true)
		}
		return parts.joinToString("&")
	}
}
