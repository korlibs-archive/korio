package com.soywiz.korio.async

suspend fun <T> Iterable<Promise<T>>.await(): List<T> = asyncFun {
	val out = arrayListOf<T>()
	for (p in this) out += p.await()
	out
}
