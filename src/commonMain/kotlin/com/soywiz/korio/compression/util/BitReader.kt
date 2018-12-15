package com.soywiz.korio.compression.util

import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*

open class BitReader(val s: AsyncInputWithLengthStream) {
	@PublishedApi
	internal var bitdata = 0
	@PublishedApi
	internal var bitsavailable = 0

	inline fun discardBits(): BitReader {
		this.bitdata = 0
		this.bitsavailable = 0
		return this
	}

	var syncSize = 0; private set
	private val sbuffers = Deque<ByteArray>()
	private var cbuffer = byteArrayOf()
	private var cbufferPos = 0

	val requirePrepare get() = syncSize < 32 * 1024

	suspend fun prepareBigChunk(): BitReader = prepareBytesUpTo(32 * 1024)

	suspend fun prepareBytesUpTo(expectedBytes: Int): BitReader {
		if (syncSize < expectedBytes) {
			val sbuffer = s.readBytesUpTo(expectedBytes)
			sbuffers += sbuffer
			syncSize += sbuffer.size
		}
		return this
	}

	fun readBits(bitcount: Int): Int {
		while (this.bitsavailable < bitcount) {
			this.bitdata = this.bitdata or (_su8() shl this.bitsavailable)
			this.bitsavailable += 8
		}
		val readed = this.bitdata and ((1 shl bitcount) - 1)
		this.bitdata = this.bitdata ushr bitcount
		this.bitsavailable -= bitcount
		return readed
	}

	fun sreadBit(): Boolean = readBits(1) != 0

	private fun _su8(): Int {
		while (cbufferPos >= cbuffer.size) {
			if (sbuffers.isEmpty()) error("sbuffer is empty!")
			val ba = sbuffers.removeFirst()
			cbuffer = ba
			cbufferPos = 0
		}
		syncSize--
		return cbuffer.readU8(cbufferPos++)
	}

	fun sbytes_noalign(count: Int, out: ByteArray): ByteArray {
		for (n in 0 until count) out[n] = _su8().toByte()
		return out
	}

	fun sbytes(count: Int): ByteArray = sbytes(count, ByteArray(count))
	fun sbytes(count: Int, out: ByteArray): ByteArray = discardBits().sbytes_noalign(count, out)
	fun su8(): Int = discardBits()._su8()
	fun su16_le(): Int = sbytes(2, temp).readU16_le(0)
	fun su32_le(): Int = sbytes(4, temp).readS32_le(0)
	fun su32_be(): Int = sbytes(4, temp).readS32_be(0)

	private val temp = ByteArray(4)
	suspend fun abytes(count: Int, out: ByteArray = ByteArray(count)) = prepareBytesUpTo(count).sbytes(count, out)

	suspend fun strz(): String {
		return MemorySyncStreamToByteArraySuspend {
			discardBits()
			while (true) {
				if (requirePrepare) prepareBigChunk()
				val c = _su8()
				if (c == 0) break
				write8(c)
			}
		}.toString(ASCII)
	}

}

