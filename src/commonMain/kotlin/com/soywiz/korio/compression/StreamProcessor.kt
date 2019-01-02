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

interface ByteReader {
	fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int
	fun readByte(): Int
}

interface ByteWriter {
	fun writeBytes(bytes: ByteArray, offset: Int, size: Int): Int
	fun writeByte(v: Int): Boolean
}

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

class RingBuffer(val bits: Int) : ByteReader, ByteWriter {
	val totalSize = 1 shl bits
	private val mask = totalSize - 1
	private val buffer = ByteArray(totalSize)
	private var readPos = 0
	private var writePos = 0
	var availableWrite = totalSize; private set
	var availableRead = 0; private set
	private val tempByte = ByteArray(1)

	override fun writeBytes(bytes: ByteArray, offset: Int, size: Int): Int {
		val toWrite = min(availableWrite, size)
		for (n in 0 until toWrite) {
			buffer[writePos] = bytes[offset + n]
			writePos = (writePos + 1) and mask
		}
		availableRead += toWrite
		availableWrite -= toWrite
		return toWrite
	}

	override fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int {
		val toRead = min(availableRead, size)
		for (n in 0 until toRead) {
			bytes[offset + n] = buffer[readPos]
			readPos = (readPos + 1) and mask
		}
		availableWrite += toRead
		availableRead -= toRead
		return toRead
	}

	override fun readByte(): Int {
		if (readBytes(tempByte, 0, 1) <= 0) return -1
		return tempByte[0].toInt() and 0xFF
	}

	override fun writeByte(v: Int): Boolean {
		tempByte[0] = v.toByte()
		return writeBytes(tempByte, 0, 1) > 0
	}

	fun clear() {
		readPos = 0
		writePos = 0
		availableRead = 0
		availableWrite = totalSize
	}
}

class ByteArrayDeque(val initialBits: Int = 10) : ByteReader, ByteWriter {
	private var ring = RingBuffer(initialBits)
	private val tempBuffer = ByteArray(1024)

	val availableWrite get() = ring.availableWrite
	val availableRead get() = ring.availableRead

	override fun writeBytes(bytes: ByteArray, offset: Int, size: Int): Int = ensureWrite(size).ring.writeBytes(bytes, offset, size)
	override fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int = ring.readBytes(bytes, offset, size)
	override fun readByte(): Int = ring.readByte()
	override fun writeByte(v: Int): Boolean = ensureWrite(1).ring.writeByte(v)

	private fun ensureWrite(count: Int): ByteArrayDeque {
		if (count > ring.availableWrite) {
			val minNewSize = ring.availableRead + count
			val newBits = ilog2(minNewSize) + 2
			val newRing = RingBuffer(newBits)
			while (ring.availableRead > 0) {
				val read = ring.readBytes(tempBuffer, 0, tempBuffer.size)
				newRing.writeBytes(tempBuffer, 0, read)
			}
			this.ring = newRing
		}
		return this
	}

	fun clear() {
		ring.clear()
	}
}
