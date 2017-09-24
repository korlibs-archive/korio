package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorker

header class SimplerMessageDigest(name: String) {
	suspend fun update(data: ByteArray, offset: Int, size: Int): Unit
	suspend fun digest(): ByteArray
}

suspend fun SimplerMessageDigest.update(data: ByteArray) = update(data, 0, data.size)
suspend fun SimplerMessageDigest.digest(data: ByteArray): ByteArray {
	update(data)
	return digest()
}

header class SimplerMac(name: String) {
	suspend fun init(key: ByteArray, algorithm: String)
	impl suspend fun update(data: ByteArray, offset: Int, size: Int)
	suspend fun finalize(): ByteArray
}

suspend fun SimplerMac.update(data: ByteArray) = update(data, 0, data.size)
suspend fun SimplerMac.finalize(data: ByteArray): ByteArray {
	update(data)
	return finalize()
}
