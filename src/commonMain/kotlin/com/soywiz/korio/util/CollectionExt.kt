package com.soywiz.korio.util

public inline fun <T, R> Iterable<T>.firstNotNullOrNull(predicate: (T) -> R?): R? {
	for (e in this) {
		val res = predicate(e)
		if (res != null) return res
	}
	return null
}
