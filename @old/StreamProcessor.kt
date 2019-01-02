package com.soywiz.korio.compression

import com.soywiz.kmem.*
import com.soywiz.korio.stream.*
import kotlin.math.*

interface MyNewCompressionMethod {
	fun createCompresor(): StreamProcessor
	fun createDecompresor(): StreamProcessor
}

fun ByteArray.compress(method: MyNewCompressionMethod): ByteArray = method.createCompresor().process(this)
fun ByteArray.uncompress(method: MyNewCompressionMethod): ByteArray = method.createDecompresor().process(this)

interface StreamProcessor {
	enum class Status { NEED_INPUT, NEED_OUTPUT, CONTINUE, FINISHED }
	val availableInput: Int
	val availableOutput: Int
	fun addInput(data: ByteArray, offset: Int, len: Int): Int
	fun inputEod()
	fun reset()
	fun process(): Status
	fun readOutput(data: ByteArray, offset: Int, len: Int): Int
}

fun ByteArray.process(method: StreamProcessor): ByteArray = method.process(this)

fun StreamProcessor.process(src: ByteArray): ByteArray {
	val dst = MemorySyncStream()
	process(src.openSync(), dst)
	return dst.toByteArray()
}

inline fun StreamProcessor.process(srcRead: (ByteArray, Int, Int) -> Int, dstWrite: (ByteArray, Int, Int) -> Unit, temp: ByteArray = ByteArray(4096)) {
	var finished = false

	reset()
	while (!finished) {
		val result = process()

		when (result) {
			StreamProcessor.Status.NEED_INPUT -> {
				val read = srcRead(temp, 0, min(temp.size, availableInput))
				if (read > 0) {
					addInput(temp, 0, read)
				} else {
					inputEod()
				}
			}
			StreamProcessor.Status.FINISHED -> {
				finished = true
			}
			else -> {
			}
		}

		do {
			val toWrite = readOutput(temp, 0, temp.size)
			//println("TO_WRITE: $toWrite")
			if (toWrite > 0) {
				dstWrite(temp, 0, toWrite)
			}
		} while (toWrite > 0)
	}
}

suspend fun StreamProcessor.process(src: AsyncInputStream, dst: AsyncOutputStream, temp: ByteArray = ByteArray(4096)) = process({ a, b, c -> src.read(a, b, c)}, { a, b, c -> dst.write(a, b, c)}, temp)
fun StreamProcessor.process(src: SyncInputStream, dst: SyncOutputStream, temp: ByteArray = ByteArray(4096)) = process({ a, b, c -> src.read(a, b, c)}, { a, b, c -> dst.write(a, b, c)}, temp)

open class BitReader(val br: ByteReader) {
	var bitdata = 0
	var bitsavailable = 0
	var peekbits = 0

	fun discardBits(): BitReader {
		bitdata = 0
		bitsavailable = 0
		peekbits = 0
		return this
	}

	inline fun drop(bitcount: Int) {
		peekbits -= bitcount
		bitdata = bitdata ushr bitcount
		bitsavailable -= bitcount
	}

	fun ensure(bitcount: Int) {
		while (bitsavailable < bitcount) {
			bitdata = bitdata or (u8() shl bitsavailable)
			bitsavailable += 8
			peekbits += 8
		}
	}

	inline fun peek(bitcount: Int): Int {
		ensure(bitcount)
		return bitdata and ((1 shl bitcount) - 1)
	}

	inline fun bits(bitcount: Int): Int = peek(bitcount).also { drop(bitcount) }

	fun bit(): Boolean = bits(1) != 0

	private fun u8(): Int = br.readByte()

	fun u16le(): Int {
		val l = u8()
		val h = u8()
		return (h shl 8) or (l)
	}

	fun u32be(): Int {
		val v3 = u8()
		val v2 = u8()
		val v1 = u8()
		val v0 = u8()
		return (v3 shl 24) or (v2 shl 16) or (v1 shl 8) or (v0)
	}

	fun readBytesAligned(bytes: ByteArray, offset: Int, len: Int): Int {
		discardBits()
		return br.readBytes(bytes, offset, len)
	}
}

