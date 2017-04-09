package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncInputOpenable
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.use
import com.soywiz.korio.util.write32_le
import com.soywiz.korio.vfs.VfsFile
import java.nio.charset.Charset
import java.security.MessageDigest

abstract class AsyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
		val CRC32 by lazy { CRC32Hash() }
	}

	suspend abstract fun hash(content: AsyncInputStream): ByteArray
	suspend fun hash(content: ByteArray): ByteArray = hash(content.openAsync())
	suspend fun hash(content: String, charset: Charset = Charsets.UTF_8): ByteArray = hash(content.toByteArray(charset))
	suspend fun hash(openable: AsyncInputOpenable): ByteArray = openable.openRead().use { hash(this) }

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

	class CRC32Hash() : AsyncHash() {
		suspend override fun hash(content: AsyncInputStream): ByteArray {
			val temp = ByteArray(0x1000)
			val crc32 = java.util.zip.CRC32()
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				crc32.update(temp, 0, read)
			}
			val out = ByteArray(4)
			out.write32_le(0, crc32.value.toInt())
			return out
		}
	}
}

suspend fun ByteArray.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputStream.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputOpenable.hash(hash: AsyncHash) = hash.hash(this)
