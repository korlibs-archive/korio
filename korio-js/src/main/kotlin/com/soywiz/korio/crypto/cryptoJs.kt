package com.soywiz.korio.crypto

actual class SimplerMessageDigest actual constructor(name: String) {
	actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	actual suspend fun digest(): ByteArray = TODO()
}

actual class SimplerMac actual constructor(name: String, key: ByteArray) {
	actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	actual suspend fun finalize(): ByteArray = TODO()
}
