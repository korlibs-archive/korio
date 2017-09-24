package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorker
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

impl class SimplerMessageDigest impl constructor(name: String) {
	val md = MessageDigest.getInstance(name)

	impl suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorker {
		md.update(data, offset, size)
	}

	impl suspend fun digest(): ByteArray = executeInWorker {
		md.digest()
	}
}

impl class SimplerMac impl constructor(name: String) {
	val mac = Mac.getInstance(name)

	impl suspend fun init(key: ByteArray, algorithm: String): Unit = executeInWorker {
		mac.init(SecretKeySpec(key, algorithm))
	}

	impl suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorker {
		mac.update(data, offset, size)
	}

	impl suspend fun finalize(): ByteArray = executeInWorker {
		mac.doFinal()
	}
}
