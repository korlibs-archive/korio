package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorkerSafer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

impl class SimplerMessageDigest impl constructor(name: String) {
	val md = MessageDigest.getInstance(name)

	impl suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
		md.update(data, offset, size)
	}

	impl suspend fun digest(): ByteArray = executeInWorkerSafer {
		md.digest()
	}
}

impl class SimplerMac impl constructor(name: String, key: ByteArray) {
	val mac = Mac.getInstance(name).apply {
		init(SecretKeySpec(key, name))
	}

	impl suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
		mac.update(data, offset, size)
	}

	impl suspend fun finalize(): ByteArray = executeInWorkerSafer {
		mac.doFinal()
	}
}
