package com.soywiz.korio.crypto

import com.soywiz.korio.async.executeInWorkerSafe
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.Charsets
import com.soywiz.korio.stream.AsyncInputOpenable
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.use
import com.soywiz.korio.util.write32_le
import java.nio.charset.Charset
import java.security.MessageDigest

abstract class AsyncHash {
	companion object {
		val MD5 by lazy { MessageDigestHash("MD5") }
		val SHA1 by lazy { MessageDigestHash("SHA1") }
		val CRC32 by lazy { CRC32Hash() }
	}

	suspend abstract fun hashSync(content: AsyncInputStream): ByteArray
	suspend fun hashSync(content: ByteArray): ByteArray = hashSync(content.openAsync())
	suspend fun hashSync(content: String, charset: Charset = Charsets.UTF_8): ByteArray = hashSync(content.toByteArray(charset))
	suspend fun hashSync(openable: AsyncInputOpenable): ByteArray = openable.openRead().use { hashSync(this) }

	suspend fun hash(content: AsyncInputStream): ByteArray = executeInWorkerSafe { hashSync(content)  }
	suspend fun hash(content: ByteArray): ByteArray = executeInWorkerSafe { hashSync(content)  }
	suspend fun hash(content: String, charset: Charset = Charsets.UTF_8): ByteArray = executeInWorkerSafe { hashSync(content)  }
	suspend fun hash(openable: AsyncInputOpenable): ByteArray = executeInWorkerSafe { hashSync(openable)  }

	class MessageDigestHash(val algo: String) : AsyncHash() {
		suspend override fun hashSync(content: AsyncInputStream): ByteArray {
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

	class CRC32Hash() : AsyncHash() {
		suspend override fun hashSync(content: AsyncInputStream): ByteArray {
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

suspend fun ByteArray.hashSync(hash: AsyncHash) = hash.hashSync(this)
suspend fun AsyncInputStream.hashSync(hash: AsyncHash) = hash.hashSync(this)
suspend fun AsyncInputOpenable.hashSync(hash: AsyncHash) = hash.hashSync(this)

suspend fun ByteArray.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputStream.hash(hash: AsyncHash) = hash.hash(this)
suspend fun AsyncInputOpenable.hash(hash: AsyncHash) = hash.hash(this)
