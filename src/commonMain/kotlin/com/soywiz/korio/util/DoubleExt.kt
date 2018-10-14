package com.soywiz.korio.util

import com.soywiz.korio.lang.*

fun Double.toString(dplaces: Int, skipTrailingZeros: Boolean = false): String {
	val res = this.toString()
	val parts = res.split('.', limit = 2)
	val integral = parts.getOrElse(0) { "0" }
	val decimal = parts.getOrElse(1) { "0" }
	if (dplaces == 0) return integral
	var out = integral + "." + (decimal + "0".repeat(dplaces)).substr(0, dplaces)
	if (skipTrailingZeros) {
		while (out.endsWith('0')) out = out.substring(0, out.length - 1)
		if (out.endsWith('.')) out = out.substring(0, out.length - 1)
	}
	return out
}
