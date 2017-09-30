package com.soywiz.korio.compression

actual object SyncCompression {
	actual fun inflate(data: ByteArray): ByteArray {
		TODO()
	}

	actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
		TODO()
	}

	actual fun deflate(data: ByteArray, level: Int): ByteArray {
		TODO()
	}
}