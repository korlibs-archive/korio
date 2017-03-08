package com.soywiz.korio.crypto

import com.soywiz.korio.stream.SyncInputStream
import com.soywiz.korio.stream.openSync
import java.nio.charset.Charset
import java.security.MessageDigest

abstract class SyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
	}

	abstract fun hash(content: SyncInputStream): ByteArray
	fun hash(content: ByteArray): ByteArray = hash(content.openSync())
	fun hash(content: String, charset: Charset = Charsets.UTF_8): ByteArray = hash(content.toByteArray(charset))

	class MessageDigestHash(val algo: String) : SyncHash() {
		override fun hash(content: SyncInputStream): ByteArray {
			val temp = ByteArray(0x1000)
			val md = MessageDigest.getInstance(algo)
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				md.update(temp, 0, read)
			}
			return md.digest()
		}
	}
}

fun ByteArray.md5() = SyncHash.MD5.hash(this)
fun ByteArray.sha1() = SyncHash.SHA1.hash(this)
