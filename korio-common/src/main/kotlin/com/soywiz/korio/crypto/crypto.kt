package com.soywiz.korio.crypto

import com.soywiz.korio.KorioNative

typealias SimplerMessageDigest = KorioNative.SimplerMessageDigest
typealias SimplerMac = KorioNative.SimplerMac

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
