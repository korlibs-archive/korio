package com.soywiz.korio.lang

interface Closeable {
	fun close(): Unit
}

fun Closeable(callback: () -> Unit) = object : Closeable {
	override fun close() = callback()
}