package com.soywiz.korio.crypto

expect class SimplerMessageDigest(name: String) {
	suspend fun update(data: ByteArray, offset: Int, size: Int): Unit
	suspend fun digest(): ByteArray
}

expect class SimplerMac(name: String, key: ByteArray) {
	suspend fun update(data: ByteArray, offset: Int, size: Int)
	suspend fun finalize(): ByteArray
}

suspend fun SimplerMessageDigest.update(data: ByteArray) = update(data, 0, data.size)
suspend fun SimplerMessageDigest.digest(data: ByteArray): ByteArray {
	update(data)
	return digest()
}

suspend fun SimplerMac.update(data: ByteArray) = update(data, 0, data.size)
suspend fun SimplerMac.finalize(data: ByteArray): ByteArray {
	update(data)
	return finalize()
}
