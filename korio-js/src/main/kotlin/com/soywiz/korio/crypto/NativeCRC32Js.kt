package com.soywiz.korio.crypto

actual class NativeCRC32 {
	actual fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	actual fun digest(): Int = TODO()
}
