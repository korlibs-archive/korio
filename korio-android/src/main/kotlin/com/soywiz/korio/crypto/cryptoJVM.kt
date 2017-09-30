package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorkerSafer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class SimplerMessageDigest actual constructor(name: String) {
	val md = MessageDigest.getInstance(name)

	actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
		md.update(data, offset, size)
	}

	actual suspend fun digest(): ByteArray = executeInWorkerSafer {
		md.digest()
	}
}

actual class SimplerMac actual constructor(name: String, key: ByteArray) {
	val mac = Mac.getInstance(name).apply {
		init(SecretKeySpec(key, name))
	}

	actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
		mac.update(data, offset, size)
	}

	actual suspend fun finalize(): ByteArray = executeInWorkerSafer {
		mac.doFinal()
	}
}
