package com.soywiz.korio.util.crypto

class SimplerMessageDigest(val name: String) {
	companion object {
		fun getInstance(name: String) = SimplerMessageDigest(name)
	}

	fun update(data: ByteArray, offset: Int = 0, count: Int = data.size): Unit = TODO()
	fun digest(): ByteArray = TODO()
}

fun SimplerMessageDigest.digest(data: ByteArray): ByteArray {
	update(data)
	return digest()
}

class Mac {
	companion object {
		fun getInstance(name: String) = Mac()
	}

	fun init(secretKeySpec: SecretKeySpec): Unit = TODO()
	fun doFinal(data: ByteArray): ByteArray = TODO()
}

class SecretKeySpec(val key: ByteArray, val algorithm: String)