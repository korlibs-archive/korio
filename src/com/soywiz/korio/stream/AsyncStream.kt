package com.soywiz.korio.stream

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.util.*
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

open class AsyncStream : AsyncCloseable {
	suspend open fun read(buffer: ByteArray, offset: Int, len: Int): Int = throw UnsupportedOperationException()
	suspend open fun write(buffer: ByteArray, offset: Int, len: Int): Unit = throw UnsupportedOperationException()
	suspend open fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
	suspend open fun getPosition(): Long = throw UnsupportedOperationException()
	suspend open fun setLength(value: Long): Unit = throw UnsupportedOperationException()
	suspend open fun getLength(): Long = throw UnsupportedOperationException()

	suspend open fun getAvailable(): Long = asyncFun { getLength() - getPosition() }
	suspend open fun eof(): Boolean = asyncFun { this.getAvailable() <= 0L }

	override suspend open fun close(): Unit = Unit

	internal val temp = ByteArray(16)
}

class SliceAsyncStream(val base: AsyncStream, val baseOffset: Long, val baseEnd: Long) : AsyncStream() {
	val baseLength = baseEnd - baseOffset
	var position = 0L

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
		val old = base.getPosition()
		base.setPosition(this.baseOffset + this.position)
		val res = base.read(buffer, offset, len)
		this.position += res
		base.setPosition(old)
		res
	}

	suspend override fun write(buffer: ByteArray, offset: Int, len: Int) = asyncFun {
		val old = base.getPosition()
		base.setPosition(this.baseOffset + this.position)
		base.write(buffer, offset, len)
		this.position += len
		base.setPosition(old)
	}

	suspend override fun setPosition(value: Long) {
		position = value
	}

	suspend override fun getPosition(): Long {
		return position
	}

	suspend override fun getLength(): Long {
		return baseLength
	}
}

suspend fun AsyncStream.sliceWithStart(start: Long): SliceAsyncStream = asyncFun { sliceWithBounds(start, this.getLength()) }

fun AsyncStream.sliceWithSize(start: Long, length: Long): SliceAsyncStream = sliceWithBounds(start, start + length)

fun AsyncStream.slice(range: IntRange): SliceAsyncStream = sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1))
fun AsyncStream.slice(range: LongRange): SliceAsyncStream = sliceWithBounds(range.start, (range.endInclusive + 1))

fun AsyncStream.sliceWithBounds(start: Long, end: Long): SliceAsyncStream {
	// @TODO: Check bounds
	return if (this is SliceAsyncStream) {
		SliceAsyncStream(this.base, this.baseOffset + start, this.baseOffset + end)
	} else {
		SliceAsyncStream(this, start, end)
	}
}

suspend fun AsyncStream.slice(): SliceAsyncStream = asyncFun { this.sliceWithSize(0L, this.getLength()) }

suspend fun AsyncStream.readSlice(length: Long): SliceAsyncStream = asyncFun {
	val start = getPosition()
	val out = this.sliceWithSize(start, length)
	setPosition(start + length)
	out
}

suspend fun AsyncStream.readStream(length: Int): SliceAsyncStream = readSlice(length.toLong())
suspend fun AsyncStream.readStream(length: Long): SliceAsyncStream = readSlice(length)

suspend fun AsyncStream.readStringz(charset: Charset = Charsets.UTF_8): String = asyncFun {
	val buf = ByteArrayOutputStream()
	while (!eof()) {
		val b = readU8()
		if (b == 0) break
		buf.write(b.toInt())
	}
	buf.toByteArray().toString(charset)
}

suspend fun AsyncStream.readStringz(len: Int, charset: Charset = Charsets.UTF_8): String = asyncFun {
	val res = readBytes(len)
	val index = res.indexOf(0.toByte())
	String(res, 0, if (index < 0) len else index, charset)
}

suspend fun AsyncStream.readString(len: Int, charset: Charset = Charsets.UTF_8): String = asyncFun { readBytes(len).toString(charset) }

suspend fun AsyncStream.writeStringz(str: String, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(charset))
suspend fun AsyncStream.writeStringz(str: String, len: Int, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(len, charset))

suspend fun AsyncStream.writeString(string: String, charset: Charset = Charsets.UTF_8): Unit = asyncFun { writeBytes(string.toByteArray(charset)) }

suspend fun AsyncStream.readExact(buffer: ByteArray, offset: Int, len: Int) = asyncFun {
	var remaining = len
	var coffset = offset
	while (remaining > 0) {
		val read = read(buffer, coffset, remaining)
		if (read < 0) break
		if (read == 0) throw IllegalStateException("Not enough data")
		coffset += read
		remaining -= read
	}
}

suspend private fun AsyncStream.readTemp(len: Int): ByteArray = asyncFun { temp.apply { readExact(temp, 0, len) } }

suspend fun AsyncStream.read(data: ByteArray): Int = read(data, 0, data.size)
suspend fun AsyncStream.read(data: UByteArray): Int = read(data.data, 0, data.size)

suspend fun AsyncStream.readBytes(len: Int): ByteArray = asyncFun { ByteArray(len).apply { read(this, 0, len) } }
suspend fun AsyncStream.readBytesExact(len: Int): ByteArray = asyncFun { ByteArray(len).apply { readExact(this, 0, len) } }

suspend fun AsyncStream.readU8(): Int = asyncFun { readTemp(1).readU8(0) }
suspend fun AsyncStream.readU16_le(): Int = asyncFun { readTemp(2).readU16_le(0) }
suspend fun AsyncStream.readU32_le(): Long = asyncFun { readTemp(4).readU32_le(0) }
suspend fun AsyncStream.readS16_le(): Int = asyncFun { readTemp(2).readS16_le(0) }
suspend fun AsyncStream.readS32_le(): Int = asyncFun { readTemp(4).readS32_le(0) }
suspend fun AsyncStream.readS64_le(): Long = asyncFun { readTemp(8).readS64_le(0) }
suspend fun AsyncStream.readF32_le(): Float = asyncFun { readTemp(4).readF32_le(0) }
suspend fun AsyncStream.readF64_le(): Double = asyncFun { readTemp(8).readF64_le(0) }
suspend fun AsyncStream.readU16_be(): Int = asyncFun { readTemp(2).readU16_be(0) }
suspend fun AsyncStream.readU32_be(): Long = asyncFun { readTemp(4).readU32_be(0) }
suspend fun AsyncStream.readS16_be(): Int = asyncFun { readTemp(2).readS16_be(0) }
suspend fun AsyncStream.readS32_be(): Int = asyncFun { readTemp(4).readS32_be(0) }
suspend fun AsyncStream.readS64_be(): Long = asyncFun { readTemp(8).readS64_be(0) }
suspend fun AsyncStream.readF32_be(): Float = asyncFun { readTemp(4).readF32_be(0) }
suspend fun AsyncStream.readF64_be(): Double = asyncFun { readTemp(8).readF64_be(0) }
suspend fun AsyncStream.readAvailable(): ByteArray = asyncFun { readBytes(getAvailable().toInt()) }
suspend fun AsyncStream.readAll(): ByteArray = asyncFun { readBytes(getAvailable().toInt()) }

private suspend inline fun <T> AsyncStream.readTypedArray(count: Int, elementSize: Int, crossinline gen: () -> T, crossinline read: (array: T, n: Int) -> Unit): T = asyncFun {
	val temp = readBytes(count * elementSize)
	val array = gen()
	for (n in 0 until count) read(array, n)
	array
}

suspend fun AsyncStream.readUByteArray(count: Int): UByteArray = asyncFun { UByteArray(readBytes(count)) }

suspend fun AsyncStream.readShortArray_le(count: Int): ShortArray = readTypedArray(count, 2, { ShortArray(count) }, { array, n -> array[n] = temp.readS16_le(n * 2).toShort() })
suspend fun AsyncStream.readShortArray_be(count: Int): ShortArray = readTypedArray(count, 2, { ShortArray(count) }, { array, n -> array[n] = temp.readS16_be(n * 2).toShort() })

suspend fun AsyncStream.readCharArray_le(count: Int): CharArray = readTypedArray(count, 2, { CharArray(count) }, { array, n -> array[n] = temp.readU16_le(n * 2).toChar() })
suspend fun AsyncStream.readCharArray_be(count: Int): CharArray = readTypedArray(count, 2, { CharArray(count) }, { array, n -> array[n] = temp.readU16_be(n * 2).toChar() })

suspend fun AsyncStream.readIntArray_le(count: Int): IntArray = readTypedArray(count, 4, { IntArray(count) }, { array, n -> array[n] = temp.readS32_le(n * 4) })
suspend fun AsyncStream.readIntArray_be(count: Int): IntArray = readTypedArray(count, 4, { IntArray(count) }, { array, n -> array[n] = temp.readS32_be(n * 4) })

suspend fun AsyncStream.readLongArray_le(count: Int): LongArray = readTypedArray(count, 8, { LongArray(count) }, { array, n -> array[n] = temp.readS64_le(n * 8) })
suspend fun AsyncStream.readLongArray_be(count: Int): LongArray = readTypedArray(count, 8, { LongArray(count) }, { array, n -> array[n] = temp.readS64_be(n * 8) })

suspend fun AsyncStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)
suspend fun AsyncStream.write8(v: Int): Unit = asyncFun { write(temp.apply { write8(0, v) }, 0, 1) }
suspend fun AsyncStream.write16_le(v: Int): Unit = asyncFun { write(temp.apply { write16_le(0, v) }, 0, 2) }
suspend fun AsyncStream.write32_le(v: Int): Unit = asyncFun { write(temp.apply { write32_le(0, v) }, 0, 4) }
suspend fun AsyncStream.write32_le(v: Long): Unit = asyncFun { write(temp.apply { write32_le(0, v) }, 0, 4) }
suspend fun AsyncStream.write64_le(v: Long): Unit = asyncFun { write(temp.apply { write64_le(0, v) }, 0, 8) }
suspend fun AsyncStream.writeF32_le(v: Float): Unit = asyncFun { write(temp.apply { writeF32_le(0, v) }, 0, 4) }
suspend fun AsyncStream.writeF64_le(v: Double): Unit = asyncFun { write(temp.apply { writeF64_le(0, v) }, 0, 8) }
suspend fun AsyncStream.write16_be(v: Int): Unit = asyncFun { write(temp.apply { write16_be(0, v) }, 0, 2) }
suspend fun AsyncStream.write32_be(v: Int): Unit = asyncFun { write(temp.apply { write32_be(0, v) }, 0, 4) }
suspend fun AsyncStream.write32_be(v: Long): Unit = asyncFun { write(temp.apply { write32_be(0, v) }, 0, 4) }
suspend fun AsyncStream.write64_be(v: Long): Unit = asyncFun { write(temp.apply { write64_be(0, v) }, 0, 8) }
suspend fun AsyncStream.writeF32_be(v: Float): Unit = asyncFun { write(temp.apply { writeF32_be(0, v) }, 0, 4) }
suspend fun AsyncStream.writeF64_be(v: Double): Unit = asyncFun { write(temp.apply { writeF64_be(0, v) }, 0, 8) }

fun SyncStream.toAsync() = object : AsyncStream() {
	val sync = this@toAsync
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker { sync.read(buffer, offset, len) }
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int) = executeInWorker { sync.write(buffer, offset, len) }
	suspend override fun setPosition(value: Long) = executeInWorker { sync.position = value }
	suspend override fun getPosition(): Long = executeInWorker { sync.position }
	suspend override fun setLength(value: Long) = executeInWorker { sync.length = value }
	suspend override fun getLength(): Long = executeInWorker { sync.length }
}


suspend fun AsyncStream.copyTo(target: AsyncStream): Unit = asyncFun {
	val chunk = ByteArray(1024)
	while (true) {
		val count = this.read(chunk)
		if (count <= 0) break
		this.write(chunk, 0, count)
	}
	Unit
}
