package com.soywiz.korio.lang

import com.soywiz.korio.ds.Queue
import com.soywiz.korio.util.toString

private val formatRegex = Regex("%([-]?\\d*)(\\w)")

fun String.format(vararg params: Any): String {
	val params = Queue(*params)
	return formatRegex.replace(this) { mr ->
		val param = params.dequeue()
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type) {
			"s" -> param.toString()
			"d" -> (param as Number).toLong().toString()
			"X" -> (param as Number).toLong().toString(16).toUpperCase()
			"x" -> (param as Number).toLong().toString(16).toLowerCase()
			else -> param.toString()
		}
		val prefix = if (size.startsWith('0')) '0' else ' '
		val asize = size.toIntOrNull()
		var str2 = str
		if (asize != null) {
			while (str2.length < asize) {
				str2 = prefix + str2
			}
		}
		str2
	}
}

fun String.splitKeep(regex: Regex): List<String> {
	val str = this
	val out = arrayListOf<String>()
	var lastPos = 0
	for (part in regex.findAll(this)) {
		val prange = part.range
		if (lastPos != prange.start) {
			out += str.substring(lastPos, prange.start)
		}
		out += str.substring(prange)
		lastPos = prange.endInclusive + 1
	}
	if (lastPos != str.length) {
		out += str.substring(lastPos)
	}
	return out
}

