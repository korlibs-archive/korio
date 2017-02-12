package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import java.nio.charset.Charset
import java.security.MessageDigest

abstract class AsyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
	}

	suspend abstract fun hash(content: AsyncInputStream): ByteArray
	suspend fun hash(content: ByteArray): ByteArray = hash(content.openAsync())
	suspend fun hash(content: String, charset: Charset = Charsets.UTF_8): ByteArray = hash(content.toByteArray(charset))

	class MessageDigestHash(val algo: String) : AsyncHash() {
		suspend override fun hash(content: AsyncInputStream): ByteArray = executeInWorker {
			val temp = ByteArray(0x1000)
			val md = MessageDigest.getInstance(algo)
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				md.update(temp, 0, read)
			}
			md.digest()
		}
	}
}

suspend fun ByteArray.md5Async() = AsyncHash.MD5.hash(this)
suspend fun ByteArray.sha1Async() = AsyncHash.SHA1.hash(this)
