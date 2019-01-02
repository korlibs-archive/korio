package com.soywiz.korio.compression.util

import com.soywiz.kmem.*
import kotlin.math.*

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

interface ByteReader {
	fun readBytes(bytes: ByteArray, offset: Int, size: Int): Int
	fun readByte(): Int
}

interface ByteWriter {
	fun writeBytes(bytes: ByteArray, offset: Int, size: Int): Int
	fun writeByte(v: Int): Boolean
}
