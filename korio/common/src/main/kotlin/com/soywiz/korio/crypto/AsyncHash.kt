package com.soywiz.korio.crypto

import com.soywiz.kmem.write32_le
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.stream.AsyncInputOpenable
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.use

abstract class AsyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
		val SHA256 by lazy { MessageDigestHash("SHA-256") }
		val CRC32 by lazy { CRC32Hash() }
	}

	suspend abstract fun hashSync(content: AsyncInputStream): ByteArray
	suspend fun hashSync(content: ByteArray): ByteArray = hashSync(content.openAsync())
	suspend fun hashSync(content: String, charset: Charset = Charsets.UTF_8): ByteArray = hashSync(content.toByteArray(charset))
	suspend fun hashSync(openable: AsyncInputOpenable): ByteArray = openable.openRead().use { hashSync(this) }

	suspend fun hash(content: AsyncInputStream): ByteArray = executeInWorker { hashSync(content) }
	suspend fun hash(content: ByteArray): ByteArray = executeInWorker { hashSync(content) }
	suspend fun hash(content: String, charset: Charset = Charsets.UTF_8): ByteArray = executeInWorker { hashSync(content) }
	suspend fun hash(openable: AsyncInputOpenable): ByteArray = executeInWorker { hashSync(openable) }

	class MessageDigestHash(val algo: String) : AsyncHash() {
		suspend override fun hashSync(content: AsyncInputStream): ByteArray {
			val temp = ByteArray(0x1000)
			val md = SimplerMessageDigest(algo)
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				md.update(temp, 0, read)
			}
			return md.digest()
		}
	}

	class CRC32Hash : AsyncHash() {
		suspend override fun hashSync(content: AsyncInputStream): ByteArray {
			val temp = ByteArray(0x1000)
			val crc32 = NativeCRC32()
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				crc32.update(temp, 0, read)
			}
			val out = ByteArray(4)
			out.write32_le(0, crc32.digest())
			return out
		}
	}
}


suspend fun ByteArray.hashSync(hash: AsyncHash) = hash.hashSync(this)
suspend fun AsyncInputStream.hashSync(hash: AsyncHash) = hash.hashSync(this)
suspend fun AsyncInputOpenable.hashSync(hash: AsyncHash) = hash.hashSync(this)

suspend fun ByteArray.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputStream.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputOpenable.hash(hash: AsyncHash) = hash.hash(this)
