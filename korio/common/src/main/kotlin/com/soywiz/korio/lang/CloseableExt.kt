package com.soywiz.korio.lang

fun Iterable<Closeable>.closeable(): Closeable = Closeable {
	for (closeable in this@closeable) closeable.close()
}
