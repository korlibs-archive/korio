package com.soywiz.korio.lang

import com.soywiz.korio.ds.Queue
import com.soywiz.korio.util.toString

private val formatRegex = Regex("%([-]?\\d+)(\\w)")

fun String.format(vararg params: Any): String {
	val params = Queue(*params)
	return formatRegex.replace(this) { mr ->
		val param = params.dequeue()
		val size = mr.groupValues[1]
		val type = mr.groupValues[2]
		val str = when (type) {
			"d" -> (param as Number).toLong().toString()
			"X" -> (param as Number).toLong().toString(16).toUpperCase()
			"x" -> (param as Number).toLong().toString(16).toLowerCase()
			else -> param.toString()
		}
		val prefix = if (size.startsWith('0')) '0' else ' '
		val asize = size.toInt()
		var str2 = str
		while (str2.length < asize) {
			str2 = prefix + str2
		}
		str2
	}
}