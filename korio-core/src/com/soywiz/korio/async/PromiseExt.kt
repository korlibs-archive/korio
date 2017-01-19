package com.soywiz.korio.async

suspend fun <T> Iterable<Promise<T>>.await(): List<T> {
	val out = arrayListOf<T>()
	for (p in this) out += p.await()
	return out
}
