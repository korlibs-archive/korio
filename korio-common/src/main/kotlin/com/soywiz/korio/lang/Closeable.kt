package com.soywiz.korio.lang

interface Closeable {
	fun close(): Unit
}

fun Closeable(callback: () -> Unit) = object : Closeable {
	override fun close() = callback()
}

fun Iterable<Closeable>.closeable(): Closeable = Closeable {
	for (closeable in this@closeable) closeable.close()
}