package com.soywiz.korio.util.crypto

class MessageDigest {
	companion object {
		fun getInstance(name: String) = MessageDigest()
	}

	fun digest(data: ByteArray): ByteArray = TODO()
}

class Mac {
	companion object {
		fun getInstance(name: String) = Mac()
	}

	fun init(secretKeySpec: SecretKeySpec): Unit = TODO()
	fun doFinal(data: ByteArray): ByteArray = TODO()
}

class SecretKeySpec(val key: ByteArray, val algorithm: String)