package com.soywiz.korio.stream

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

class FastByteArrayInputStream(val ba: ByteArray, var offset: Int = 0) {
	val length: Int get() = ba.size
	val available: Int get() = ba.size - offset
	val hasMore: Boolean get() = available > 0
	val eof: Boolean get() = !hasMore

	// Skipping
	fun skip(count: Int) = run { offset += count }

	fun skipToAlign(count: Int) {
		val nextPosition = offset.nextAlignedTo(offset)
		readBytes((nextPosition - offset).toInt())
	}

	// 8 bit
	fun readS8() = ba[offset++]

	fun readU8() = ba[offset++].toInt() and 0xFF

	// 16 bits
	fun readS16_le() = increment(2) { ba.readS16_le(offset) }

	fun readS16_be() = increment(2) { ba.readS16_be(offset) }
	fun readU16_le() = increment(2) { ba.readU16_le(offset) }
	fun readU16_be() = increment(2) { ba.readU16_be(offset) }

	// 24 bits
	fun readS24_le() = increment(3) { ba.readS24_le(offset) }

	fun readS24_be() = increment(3) { ba.readS24_be(offset) }
	fun readU24_le() = increment(3) { ba.readU24_le(offset) }
	fun readU24_be() = increment(3) { ba.readU24_be(offset) }

	// 32 bits
	fun readS32_le() = increment(4) { ba.readS32_le(offset) }

	fun readS32_be() = increment(4) { ba.readS32_be(offset) }
	fun readU32_le() = increment(4) { ba.readU32_le(offset) }
	fun readU32_be() = increment(4) { ba.readU32_be(offset) }

	// 32 bits FLOAT
	fun readF32_le() = increment(4) { ba.readF32_le(offset) }

	fun readF32_be() = increment(4) { ba.readF32_be(offset) }

	// 64 bits FLOAT
	fun readF64_le() = increment(8) { ba.readF64_le(offset) }

	fun readF64_be() = increment(8) { ba.readF64_be(offset) }

	// Bytes
	fun readBytes(count: Int) = increment(count) { ba.readByteArray(offset, count) }

	fun readUBytes(count: Int) = UByteArray(readBytes(count))

	// Arrays
	fun readShortArray_le(count: Int): ShortArray = increment(count * 2) { ba.readShortArray_le(offset, count) }

	fun readShortArray_be(count: Int): ShortArray = increment(count * 2) { ba.readShortArray_be(offset, count) }

	fun readCharArray_le(count: Int): CharArray = increment(count * 2) { ba.readCharArray_le(offset, count) }
	fun readCharArray_be(count: Int): CharArray = increment(count * 2) { ba.readCharArray_be(offset, count) }

	fun readIntArray_le(count: Int): IntArray = increment(count * 4) { ba.readIntArray_le(offset, count) }
	fun readIntArray_be(count: Int): IntArray = increment(count * 4) { ba.readIntArray_be(offset, count) }

	fun readLongArray_le(count: Int): LongArray = increment(count * 8) { ba.readLongArray_le(offset, count) }
	fun readLongArray_be(count: Int): LongArray = increment(count * 8) { ba.readLongArray_be(offset, count) }

	fun readFloatArray_le(count: Int): FloatArray = increment(count * 4) { ba.readFloatArray_le(offset, count) }
	fun readFloatArray_be(count: Int): FloatArray = increment(count * 4) { ba.readFloatArray_be(offset, count) }

	fun readDoubleArray_le(count: Int): DoubleArray = increment(count * 8) { ba.readDoubleArray_le(offset, count) }
	fun readDoubleArray_be(count: Int): DoubleArray = increment(count * 8) { ba.readDoubleArray_be(offset, count) }

	// Variable Length
	fun readU_VL(): Int {
		var result = readU8()
		if ((result and 0x80) == 0) return result
		result = (result and 0x7f) or (readU8() shl 7)
		if ((result and 0x4000) == 0) return result
		result = (result and 0x3fff) or (readU8() shl 14)
		if ((result and 0x200000) == 0) return result
		result = (result and 0x1fffff) or (readU8() shl 21)
		if ((result and 0x10000000) == 0) return result
		result = (result and 0xfffffff) or (readU8() shl 28)
		return result
	}

	fun readS_VL(): Int {
		val v = readU_VL()
		val sign = ((v and 1) != 0)
		val uvalue = v ushr 1
		return if (sign) -uvalue - 1 else uvalue
	}


	// String
	fun readString(len: Int, charset: Charset = UTF8) = readBytes(len).toString(charset)

	fun readStringz(len: Int, charset: Charset = UTF8): String {
		val res = readBytes(len)
		val index = res.indexOf(0.toByte())
		return res.copyOf(if (index < 0) len else index).toString(charset)
	}

	fun readStringz(charset: Charset = UTF8): String {
		val startOffset = offset
		val index = ba.indexOf(offset, 0.toByte())
		val end = if (index >= 0) index else ba.size
		val str = ba.copyOfRange(startOffset, end - startOffset).toString(charset)
		offset = if (index >= 0) end + 1 else end
		return str
	}

	fun readStringVL(charset: Charset = UTF8): String = readString(readU_VL(), charset)

	// Tools
	inline private fun <T> increment(count: Int, callback: () -> T): T {
		val out = callback()
		offset += count
		return out
	}
}

fun ByteArray.openFastStream(offset: Int = 0) = FastByteArrayInputStream(this, offset)
