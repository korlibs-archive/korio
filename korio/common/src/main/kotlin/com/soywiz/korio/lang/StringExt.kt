package com.soywiz.korio.lang

import com.soywiz.korio.util.toStringUnsigned

private val formatRegex = Regex("%([-]?\\d+)?(\\w)")

fun String.format(vararg params: Any): String {
	var paramIndex = 0
	return formatRegex.replace(this) { mr ->
		val param = params[paramIndex++]
		//println("param: $param")
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type) {
			"d" -> (param as Number).toLong().toString()
			"X", "x" -> {
				val res = when (param) {
					is Int -> param.toStringUnsigned(16)
					else -> (param as Number).toLong().toStringUnsigned(16)
				}
				if (type == "X") res.toUpperCase() else res.toLowerCase()
			}
			else -> "$param"
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

private val replaceNonPrintableCharactersRegex by lazy { Regex("[^ -~]") }
fun String.replaceNonPrintableCharacters(replacement: String = "?"): String {
	return this.replace(replaceNonPrintableCharactersRegex, replacement)
}
