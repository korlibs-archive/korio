package com.soywiz.korio.lang

fun Closeable(callback: () -> Unit) = object : Closeable {
	override fun close() = callback()
}
