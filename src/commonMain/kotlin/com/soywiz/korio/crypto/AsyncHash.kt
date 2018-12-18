package com.soywiz.korio.crypto

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

abstract class AsyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
		val SHA256 by lazy { MessageDigestHash("SHA-256") }
		val CRC32 by lazy { CRC32Hash() }
	}

	abstract suspend fun hashSync(content: AsyncInputStream): ByteArray
	suspend fun hashSync(content: ByteArray): ByteArray = hashSync(content.openAsync())
	suspend fun hashSync(content: String, charset: Charset = UTF8): ByteArray =
		hashSync(content.toByteArray(charset))

	suspend fun hashSync(openable: AsyncInputOpenable): ByteArray = openable.openRead().use { hashSync(this) }

	suspend fun hash(content: AsyncInputStream): ByteArray = executeInWorker { hashSync(content) }
	suspend fun hash(content: ByteArray): ByteArray = executeInWorker { hashSync(content) }
	suspend fun hash(content: String, charset: Charset = UTF8): ByteArray =
		executeInWorker { hashSync(content) }

	suspend fun hash(openable: AsyncInputOpenable): ByteArray = executeInWorker { hashSync(openable) }

	class MessageDigestHash(val algo: String) : AsyncHash() {
		override suspend fun hashSync(content: AsyncInputStream): ByteArray {
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
		override suspend fun hashSync(content: AsyncInputStream): ByteArray {
			val temp = ByteArray(0x1000)
			var crc32 = com.soywiz.korio.crypto.CRC32.INITIAL
			while (true) {
				val read = content.read(temp, 0, temp.size)
				if (read <= 0) break
				crc32 = com.soywiz.korio.crypto.CRC32.update(crc32, temp, 0, read)
			}
			val out = ByteArray(4)
			out.write32LE(0, crc32)
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
