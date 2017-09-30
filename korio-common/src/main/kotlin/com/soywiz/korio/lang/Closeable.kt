package com.soywiz.korio.lang

interface Closeable {
	fun close(): Unit
}

fun Closeable(callback: () -> Unit) = object : Closeable {
	override fun close() = callback()
}

//java.lang.NoClassDefFoundError: com/soywiz/korio/lang/CloseableKt$Closeable$1 (wrong name: com/soywiz/korio/lang/CloseableKt$closeable$1)
//  at java.lang.ClassLoader.defineClass1(Native Method)
//fun Iterable<Closeable>.closeable(): Closeable = Closeable {
//	for (closeable in this@closeable) closeable.close()
//}

fun <TCloseable : Closeable, T : Any> TCloseable.use(callback: (TCloseable) -> T): T {
	try {
		return callback(this)
	} finally {
		this.close()
	}
}
