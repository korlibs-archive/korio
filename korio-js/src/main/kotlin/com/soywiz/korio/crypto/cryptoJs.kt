package com.soywiz.korio.crypto

impl class SimplerMessageDigest impl constructor(name: String) {
	impl suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	impl suspend fun digest(): ByteArray = TODO()
}

impl class SimplerMac impl constructor(name: String, key: ByteArray) {
	impl suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
	impl suspend fun finalize(): ByteArray = TODO()
}
