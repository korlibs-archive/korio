@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.stream

import com.soywiz.korio.EOFException
import com.soywiz.korio.KorioNative
import com.soywiz.korio.async.AsyncThread
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.ds.ByteArrayBuilder
import com.soywiz.korio.ds.ByteArrayBuilderSmall
import com.soywiz.korio.lang.*
import com.soywiz.korio.lang.tl.threadLocal
import com.soywiz.korio.typedarray.copyRangeTo
import com.soywiz.korio.typedarray.fill
import com.soywiz.korio.util.*
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korio.vfs.VfsOpenMode
import kotlin.math.min

//interface SmallTemp {
//	val smallTemp: ByteArray
//}

//interface AsyncBaseStream : AsyncCloseable, SmallTemp {
interface AsyncBaseStream : AsyncCloseable {
}

interface AsyncInputOpenable {
	suspend fun openRead(): AsyncInputStream
}

interface AsyncInputStream : AsyncBaseStream {
	suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncOutputStream : AsyncBaseStream {
	suspend fun write(buffer: ByteArray, offset: Int = 0, len: Int = buffer.size - offset)
}

interface AsyncPositionStream : AsyncBaseStream {
	suspend fun setPosition(value: Long): Unit = throw UnsupportedOperationException()
	suspend fun getPosition(): Long = throw UnsupportedOperationException()
}

interface AsyncLengthStream : AsyncBaseStream {
	suspend fun setLength(value: Long): Unit = throw UnsupportedOperationException()
	suspend fun getLength(): Long = throw UnsupportedOperationException()
}

interface AsyncPositionLengthStream : AsyncPositionStream, AsyncLengthStream {
}

interface AsyncRAInputStream {
	suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface AsyncRAOutputStream {
	suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit
}

fun AsyncBaseStream.toAsyncStream(): AsyncStream {
	val input = this as? AsyncInputStream
	val output = this as? AsyncOutputStream
	val rlen = this as? AsyncLengthStream
	val closeable = this

	return object : AsyncStreamBase() {
		var expectedPosition: Long = 0L
		//val events = arrayListOf<String>()

		suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
			if (input == null) throw UnsupportedOperationException()
			//events += "before_read:actualPosition=$position,position=$expectedPosition"
			checkPosition(position)
			val read = input.read(buffer, offset, len)
			//events += "read:$read"
			if (read > 0) expectedPosition += read
			return read
		}

		suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
			if (output == null) throw UnsupportedOperationException()
			checkPosition(position)
			output.write(buffer, offset, len)
			expectedPosition += len
		}

		private fun checkPosition(position: Long) {
			if (position != expectedPosition) {
				throw UnsupportedOperationException("Seeking not supported!")
			}
		}

		suspend override fun setLength(value: Long) = rlen?.setLength(value) ?: throw UnsupportedOperationException()
		suspend override fun getLength(): Long = rlen?.getLength() ?: throw UnsupportedOperationException()
		suspend override fun close() = closeable.close()
	}.toAsyncStream()
}

open class AsyncStreamBase : AsyncCloseable, AsyncRAInputStream, AsyncRAOutputStream, AsyncLengthStream {
	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = throw UnsupportedOperationException()
	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = throw UnsupportedOperationException()

	suspend override fun setLength(value: Long): Unit = throw UnsupportedOperationException()
	suspend override fun getLength(): Long = throw UnsupportedOperationException()

	suspend override fun close(): Unit = Unit
}

fun AsyncStreamBase.toAsyncStream(position: Long = 0L): AsyncStream = AsyncStream(this, position)

class AsyncStream(val base: AsyncStreamBase, var position: Long = 0L) : Extra by Extra.Mixin(), AsyncInputStream, AsyncOutputStream, AsyncPositionLengthStream, AsyncCloseable {
	// NOTE: Sharing queue would hang writting on hang read
	//private val ioQueue = AsyncThread()
	//private val readQueue = ioQueue
	//private val writeQueue = ioQueue

	private val readQueue = AsyncThread()
	private val writeQueue = AsyncThread()

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue {
		//suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		val read = base.read(position, buffer, offset, len)
		if (read >= 0) position += read
		read
	}

	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
		//suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit {
		base.write(position, buffer, offset, len)
		position += len
	}

	suspend override fun setPosition(value: Long): Unit = run { this.position = value }
	suspend override fun getPosition(): Long = this.position
	suspend override fun setLength(value: Long): Unit = base.setLength(value)
	suspend override fun getLength(): Long = base.getLength()
	suspend fun size(): Long = base.getLength()

	suspend fun getAvailable(): Long {
		return getLength() - getPosition()
	}

	suspend fun eof(): Boolean {
		return this.getAvailable() <= 0L
	}

	suspend override fun close(): Unit = base.close()

	suspend fun clone(): AsyncStream = AsyncStream(base, position)
}

suspend fun AsyncPositionLengthStream.getAvailable(): Long = getLength() - getPosition()
suspend fun AsyncPositionLengthStream.eof(): Boolean = this.getAvailable() <= 0L

class SliceAsyncStreamBase(internal val base: AsyncStreamBase, internal val baseStart: Long, internal val baseEnd: Long) : AsyncStreamBase() {
	internal val baseLength = baseEnd - baseStart

	private fun clampPosition(position: Long) = position.clamp(baseStart, baseEnd)

	private fun clampPositionLen(position: Long, len: Int): Pair<Long, Int> {
		if (position < 0L) throw IllegalArgumentException("Invalid position")
		val targetStartPosition = clampPosition(this.baseStart + position)
		val targetEndPosition = clampPosition(targetStartPosition + len)
		val targetLen = (targetEndPosition - targetStartPosition).toInt()
		return Pair(targetStartPosition, targetLen)
	}

	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.read(targetStartPosition, buffer, offset, targetLen)
	}

	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.write(targetStartPosition, buffer, offset, targetLen)
	}

	suspend override fun getLength(): Long = baseLength

	suspend override fun close() = Unit

	override fun toString(): String = "SliceAsyncStreamBase($base, $baseStart, $baseEnd)"
}

fun AsyncStream.buffered(blockSize: Int = 2048) = BufferedStreamBase(this.base, blockSize).toAsyncStream(this.position)

class BufferedStreamBase(val base: AsyncStreamBase, val blockSize: Int = 2048) : AsyncStreamBase() {
	fun getSectorPosition(sector: Long): Long = sector * blockSize
	fun getSectorAtPosition(position: Long): Long = position / blockSize

	inner class CachedEntry(val startSector: Long, val endSector: Long, val data: ByteArray) {
		val startPosition = getSectorPosition(startSector)
		val endPosition = getSectorPosition(endSector)

		fun getPositionInData(position: Long): Int = (position - startPosition).toIntSafe()
		fun getAvailableAtPosition(position: Long): Int = data.size - getPositionInData(position)

		fun containsSectors(startSector: Long, endSector: Long): Boolean = (startSector >= this.startSector && endSector <= this.endSector)
	}

	suspend fun readSectorsUncached(start: Long, end: Long): ByteArray {
		val length = end - start
		val out = ByteArray((blockSize * length).toInt())
		val r = base.read(getSectorPosition(start), out, 0, out.size)
		return out.copyOf(r)
	}

	var cached: CachedEntry? = null

	suspend fun readSectorsCached(start: Long, end: Long): CachedEntry {
		if (!(cached?.containsSectors(start, end) ?: false)) {
			cached = CachedEntry(start, end, readSectorsUncached(start, end))
		}
		return cached!!
	}

	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val entry = readSectorsCached(getSectorAtPosition(position), getSectorAtPosition(position + len) + 1)
		val readOffset = entry.getPositionInData(position)
		val readLen = min(entry.getAvailableAtPosition(position), len)
		entry.data.copyRangeTo(readOffset, buffer, offset, readLen)
		return readLen
	}

	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		base.write(position, buffer, offset, len)
	}

	suspend override fun setLength(value: Long) = base.setLength(value)
	suspend override fun getLength(): Long = base.getLength()
	suspend override fun close() = base.close()
}

suspend fun AsyncStream.sliceWithStart(start: Long): AsyncStream {
	return sliceWithBounds(start, this.getLength())
}

suspend fun AsyncStream.sliceWithSize(start: Long, length: Long): AsyncStream = sliceWithBounds(start, start + length)

suspend fun AsyncStream.slice(range: IntRange): AsyncStream = sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1))
suspend fun AsyncStream.slice(range: LongRange): AsyncStream = sliceWithBounds(range.start, (range.endInclusive + 1))

suspend fun AsyncStream.sliceWithBounds(start: Long, end: Long): AsyncStream {
	val len = this.getLength()
	val clampedStart = start.clamp(0, len)
	val clampedEnd = end.clamp(0, len)

	return if (this.base is SliceAsyncStreamBase) {
		SliceAsyncStreamBase(this.base.base, this.base.baseStart + clampedStart, this.base.baseStart + clampedEnd).toAsyncStream()
	} else {
		SliceAsyncStreamBase(this.base, clampedStart, clampedEnd).toAsyncStream()
	}
}

suspend fun AsyncStream.slice(): AsyncStream = this.sliceWithSize(0L, this.getLength())

suspend fun AsyncStream.readSlice(length: Long): AsyncStream {
	val start = getPosition()
	val out = this.sliceWithSize(start, length)
	setPosition(start + length)
	return out
}

suspend fun AsyncStream.readStream(length: Int): AsyncStream = readSlice(length.toLong())
suspend fun AsyncStream.readStream(length: Long): AsyncStream = readSlice(length)

suspend fun AsyncInputStream.readStringz(charset: Charset = Charsets.UTF_8): String {
	val buf = ByteArrayBuilder()
	val temp = ByteArray(1)
	while (true) {
		val read = read(temp, 0, 1)
		if (read <= 0) break
		if (temp[0] == 0.toByte()) break
		buf.append(temp[0])
	}
	return buf.toByteArray().toString(charset)
}

suspend fun AsyncInputStream.readStringz(len: Int, charset: Charset = Charsets.UTF_8): String {
	val res = readBytesExact(len)
	val index = res.indexOf(0.toByte())
	return res.copyOf(if (index < 0) len else index).toString(charset)
}

suspend fun AsyncInputStream.readString(len: Int, charset: Charset = Charsets.UTF_8): String = readBytesExact(len).toString(charset)

suspend fun AsyncOutputStream.writeStringz(str: String, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(charset))
suspend fun AsyncOutputStream.writeStringz(str: String, len: Int, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(len, charset))

suspend fun AsyncOutputStream.writeString(string: String, charset: Charset = Charsets.UTF_8): Unit = writeBytes(string.toByteArray(charset))

suspend fun AsyncInputStream.readExact(buffer: ByteArray, offset: Int, len: Int) {
	var remaining = len
	var coffset = offset
	while (remaining > 0) {
		val read = read(buffer, coffset, remaining)
		if (read < 0) break
		if (read == 0) throw com.soywiz.korio.EOFException("Not enough data")
		coffset += read
		remaining -= read
	}
}

//val READ_SMALL_TEMP by threadLocal { ByteArray(8) }
//suspend private fun AsyncInputStream.readSmallTempExact(len: Int, temp: ByteArray): ByteArray = temp.apply { readExact(temp, 0, len) }
//suspend private fun AsyncInputStream.readSmallTempExact(len: Int): ByteArray = readSmallTempExact(len, READ_SMALL_TEMP)

suspend private fun AsyncInputStream.readSmallTempExact(len: Int): ByteArray = readBytesExact(len)


suspend private fun AsyncInputStream.readTempExact(len: Int, temp: ByteArray): ByteArray = temp.apply { readExact(temp, 0, len) }
//suspend private fun AsyncInputStream.readTempExact(len: Int): ByteArray = readTempExact(len, BYTES_TEMP)

suspend fun AsyncInputStream.read(data: ByteArray): Int = read(data, 0, data.size)
suspend fun AsyncInputStream.read(data: UByteArray): Int = read(data.data, 0, data.size)

@Deprecated("Use readBytesUpTo instead", ReplaceWith("readBytesUpTo(len)"))
suspend fun AsyncInputStream.readBytes(len: Int): ByteArray = readBytesUpToFirst(len)

val EMPTY_BYTE_ARRAY = ByteArray(0)

suspend fun AsyncInputStream.readBytesUpToFirst(len: Int): ByteArray {
	val out = ByteArray(len)
	val read = read(out, 0, len)
	if (read <= 0) return EMPTY_BYTE_ARRAY
	return out.copyOf(read)
}

suspend fun AsyncInputStream.readBytesUpTo(len: Int): ByteArray {
	val BYTES_TEMP_SIZE = 0x1000
	if (len > BYTES_TEMP_SIZE) {
		if (this is AsyncPositionLengthStream) {
			val alen = min(len, this.getAvailable().toIntClamp())
			val ba = ByteArray(alen)
			var available = alen
			var pos = 0
			while (true) {
				val alen2 = read(ba, pos, available)
				if (alen2 <= 0) break
				pos += alen2
				available -= alen2
			}
			return if (ba.size == pos) ba else ba.copyOf(pos)
		} else {
			// @TODO: We can read chunks of data in preallocated byte arrays, then join them all.
			// @TODO: That would prevent resizing issues with the trade-off of more allocations.
			var pending = len
			val temp = ByteArray(BYTES_TEMP_SIZE)
			val bout = ByteArrayBuilder()
			while (pending > 0) {
				val read = this.read(temp, 0, min(temp.size, pending))
				if (read <= 0) break
				bout.append(temp, 0, read)
				pending -= read
			}
			return bout.toByteArray()
		}
	} else {
		val ba = ByteArray(len)
		var available = len
		var pos = 0
		while (true) {
			val rlen = read(ba, pos, available)
			if (rlen <= 0) break
			pos += rlen
			available -= rlen
		}
		return if (ba.size == pos) ba else ba.copyOf(pos)
	}

}

suspend fun AsyncInputStream.readBytesExact(len: Int): ByteArray = ByteArray(len).apply { readExact(this, 0, len) }

//suspend fun AsyncInputStream.readU8(): Int = readBytesExact(1).readU8(0)
suspend fun AsyncInputStream.readU8(): Int = readSmallTempExact(1).readU8(0)

suspend fun AsyncInputStream.readS8(): Int = readSmallTempExact(1).readS8(0)
suspend fun AsyncInputStream.readU16_le(): Int = readSmallTempExact(2).readU16_le(0)
suspend fun AsyncInputStream.readU24_le(): Int = readSmallTempExact(3).readU24_le(0)
suspend fun AsyncInputStream.readU32_le(): Long = readSmallTempExact(4).readU32_le(0)
suspend fun AsyncInputStream.readS16_le(): Int = readSmallTempExact(2).readS16_le(0)
suspend fun AsyncInputStream.readS24_le(): Int = readSmallTempExact(3).readS24_le(0)
suspend fun AsyncInputStream.readS32_le(): Int = readSmallTempExact(4).readS32_le(0)
suspend fun AsyncInputStream.readS64_le(): Long = readSmallTempExact(8).readS64_le(0)
suspend fun AsyncInputStream.readF32_le(): Float = readSmallTempExact(4).readF32_le(0)
suspend fun AsyncInputStream.readF64_le(): Double = readSmallTempExact(8).readF64_le(0)
suspend fun AsyncInputStream.readU16_be(): Int = readSmallTempExact(2).readU16_be(0)
suspend fun AsyncInputStream.readU24_be(): Int = readSmallTempExact(3).readU24_be(0)
suspend fun AsyncInputStream.readU32_be(): Long = readSmallTempExact(4).readU32_be(0)
suspend fun AsyncInputStream.readS16_be(): Int = readSmallTempExact(2).readS16_be(0)
suspend fun AsyncInputStream.readS24_be(): Int = readSmallTempExact(3).readS24_be(0)
suspend fun AsyncInputStream.readS32_be(): Int = readSmallTempExact(4).readS32_be(0)
suspend fun AsyncInputStream.readS64_be(): Long = readSmallTempExact(8).readS64_be(0)
suspend fun AsyncInputStream.readF32_be(): Float = readSmallTempExact(4).readF32_be(0)
suspend fun AsyncInputStream.readF64_be(): Double = readSmallTempExact(8).readF64_be(0)
suspend fun AsyncStream.hasLength(): Boolean = try {
	getLength(); true
} catch (t: Throwable) {
	false
}

suspend fun AsyncStream.hasAvailable(): Boolean = try {
	getAvailable(); true
} catch (t: Throwable) {
	false
}

suspend fun AsyncInputStream.readAll(): ByteArray {
	return try {
		if (this is AsyncStream && this.hasAvailable()) {
			val available = this.getAvailable().toInt()
			return this.readBytesExact(available)
		} else {
			val out = ByteArrayBuilder()
			val temp = ByteArray(0x1000)
			while (true) {
				val r = this.read(temp, 0, temp.size)
				if (r <= 0) break
				out.append(temp, 0, r)
			}
			out.toByteArray()
		}
	} finally {
		this.close()
	}
}

// readAll alias
suspend fun AsyncInputStream.readAvailable(): ByteArray = readAll()

suspend fun AsyncInputStream.skip(count: Int) {
	if (this is AsyncPositionLengthStream) {
		this.setPosition(this.getPosition() + count)
	} else {
		val temp = ByteArray(0x1000)
		var remaining = count
		while (remaining > 0) {
			val toRead = min(remaining, count)
			readTempExact(toRead, temp)
			remaining -= toRead
		}
	}
}

suspend fun AsyncInputStream.readUByteArray(count: Int): UByteArray = UByteArray(readBytesExact(count))

suspend fun AsyncInputStream.readShortArray_le(count: Int): ShortArray = readBytesExact(count * 2).readShortArray_le(0, count)
suspend fun AsyncInputStream.readShortArray_be(count: Int): ShortArray = readBytesExact(count * 2).readShortArray_be(0, count)

suspend fun AsyncInputStream.readCharArray_le(count: Int): CharArray = readBytesExact(count * 2).readCharArray_le(0, count)
suspend fun AsyncInputStream.readCharArray_be(count: Int): CharArray = readBytesExact(count * 2).readCharArray_be(0, count)

suspend fun AsyncInputStream.readIntArray_le(count: Int): IntArray = readBytesExact(count * 4).readIntArray_le(0, count)
suspend fun AsyncInputStream.readIntArray_be(count: Int): IntArray = readBytesExact(count * 4).readIntArray_be(0, count)

suspend fun AsyncInputStream.readLongArray_le(count: Int): LongArray = readBytesExact(count * 8).readLongArray_le(0, count)
suspend fun AsyncInputStream.readLongArray_be(count: Int): LongArray = readBytesExact(count * 8).readLongArray_le(0, count)

suspend fun AsyncInputStream.readFloatArray_le(count: Int): FloatArray = readBytesExact(count * 4).readFloatArray_le(0, count)
suspend fun AsyncInputStream.readFloatArray_be(count: Int): FloatArray = readBytesExact(count * 4).readFloatArray_be(0, count)

suspend fun AsyncInputStream.readDoubleArray_le(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArray_le(0, count)
suspend fun AsyncInputStream.readDoubleArray_be(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArray_be(0, count)

suspend fun AsyncOutputStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)
suspend fun AsyncOutputStream.writeBytes(data: ByteArraySlice): Unit = write(data.data, data.position, data.length)
suspend fun AsyncOutputStream.write8(v: Int): Unit = write(ByteArray(1).apply { this@apply.write8(0, v) }, 0, 1)
suspend fun AsyncOutputStream.write16_le(v: Int): Unit = write(ByteArray(2).apply { this@apply.write16_le(0, v) }, 0, 2)
suspend fun AsyncOutputStream.write24_le(v: Int): Unit = write(ByteArray(3).apply { this@apply.write24_le(0, v) }, 0, 3)
suspend fun AsyncOutputStream.write32_le(v: Int): Unit = write(ByteArray(4).apply { this@apply.write32_le(0, v) }, 0, 4)
suspend fun AsyncOutputStream.write32_le(v: Long): Unit = write(ByteArray(4).apply { this@apply.write32_le(0, v) }, 0, 4)
suspend fun AsyncOutputStream.write64_le(v: Long): Unit = write(ByteArray(8).apply { this@apply.write64_le(0, v) }, 0, 8)
suspend fun AsyncOutputStream.writeF32_le(v: Float): Unit = write(ByteArray(4).apply { this@apply.writeF32_le(0, v) }, 0, 4)
suspend fun AsyncOutputStream.writeF64_le(v: Double): Unit = write(ByteArray(8).apply { this@apply.writeF64_le(0, v) }, 0, 8)
suspend fun AsyncOutputStream.write16_be(v: Int): Unit = write(ByteArray(2).apply { this@apply.write16_be(0, v) }, 0, 2)
suspend fun AsyncOutputStream.write24_be(v: Int): Unit = write(ByteArray(3).apply { this@apply.write24_be(0, v) }, 0, 3)
suspend fun AsyncOutputStream.write32_be(v: Int): Unit = write(ByteArray(4).apply { this@apply.write32_be(0, v) }, 0, 4)
suspend fun AsyncOutputStream.write32_be(v: Long): Unit = write(ByteArray(4).apply { this@apply.write32_be(0, v) }, 0, 4)
suspend fun AsyncOutputStream.write64_be(v: Long): Unit = write(ByteArray(8).apply { this@apply.write64_be(0, v) }, 0, 8)
suspend fun AsyncOutputStream.writeF32_be(v: Float): Unit = write(ByteArray(4).apply { this@apply.writeF32_be(0, v) }, 0, 4)
suspend fun AsyncOutputStream.writeF64_be(v: Double): Unit = write(ByteArray(8).apply { this@apply.writeF64_be(0, v) }, 0, 8)

fun SyncStream.toAsync(): AsyncStream = this.base.toAsync().toAsyncStream(this.position)
fun SyncStreamBase.toAsync(): AsyncStreamBase = SyncAsyncStreamBase(this)

fun SyncStream.toAsyncInWorker(): AsyncStream = this.base.toAsyncInWorker().toAsyncStream(this.position)
fun SyncStreamBase.toAsyncInWorker(): AsyncStreamBase = SyncAsyncStreamBaseInWorker(this)

class SyncAsyncStreamBase(val sync: SyncStreamBase) : AsyncStreamBase() {
	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = sync.read(position, buffer, offset, len)
	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = sync.write(position, buffer, offset, len)
	suspend override fun setLength(value: Long) = run { sync.length = value }
	suspend override fun getLength(): Long = sync.length
}

class SyncAsyncStreamBaseInWorker(val sync: SyncStreamBase) : AsyncStreamBase() {
	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker { sync.read(position, buffer, offset, len) }
	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = executeInWorker { sync.write(position, buffer, offset, len) }
	suspend override fun setLength(value: Long) = executeInWorker { sync.length = value }
	suspend override fun getLength(): Long = executeInWorker { sync.length }
}

suspend fun AsyncOutputStream.writeStream(source: AsyncInputStream): Long = source.copyTo(this)

suspend fun AsyncOutputStream.writeFile(source: VfsFile): Long {
	val out = this@writeFile
	return source.openUse(VfsOpenMode.READ) { out.writeStream(this) }
}

suspend fun AsyncInputStream.copyTo(target: AsyncOutputStream): Long {
	val chunk = ByteArray(0x1000)
	var totalCount = 0L

	//if (this is AsyncPositionLengthStream) {
	//	println("Position: " + this.getPosition())
	//	println("Length: " + this.getLength())
	//	println("Available: " + this.getAvailable())
	//}

	while (true) {
		val count = this.read(chunk)
		if (count <= 0) break
		target.write(chunk, 0, count)
		totalCount += count
	}
	//println("Copied: $totalCount, chunkSize: ${chunk.size}")
	//Unit
	return totalCount
}

suspend fun AsyncStream.writeToAlign(alignment: Int, value: Int = 0) {
	val nextPosition = getPosition().nextAlignedTo(alignment.toLong())
	val data = ByteArray((nextPosition - getPosition()).toInt())
	data.fill(value.toByte())
	writeBytes(data)
}

suspend fun AsyncStream.skip(count: Int): AsyncStream {
	position += count
	return this
}

suspend fun AsyncStream.skipToAlign(alignment: Int) {
	val nextPosition = getPosition().nextAlignedTo(alignment.toLong())
	readBytes((nextPosition - getPosition()).toInt())
}

suspend fun AsyncStream.truncate() = setLength(getPosition())

suspend fun AsyncOutputStream.writeCharArray_le(array: CharArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_le(0, array) })
suspend fun AsyncOutputStream.writeShortArray_le(array: ShortArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_le(0, array) })
suspend fun AsyncOutputStream.writeIntArray_le(array: IntArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_le(0, array) })
suspend fun AsyncOutputStream.writeLongArray_le(array: LongArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_le(0, array) })
suspend fun AsyncOutputStream.writeFloatArray_le(array: FloatArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_le(0, array) })
suspend fun AsyncOutputStream.writeDoubleArray_le(array: DoubleArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_le(0, array) })

suspend fun AsyncOutputStream.writeCharArray_be(array: CharArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_be(0, array) })
suspend fun AsyncOutputStream.writeShortArray_be(array: ShortArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_be(0, array) })
suspend fun AsyncOutputStream.writeIntArray_be(array: IntArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_be(0, array) })
suspend fun AsyncOutputStream.writeLongArray_be(array: LongArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_be(0, array) })
suspend fun AsyncOutputStream.writeFloatArray_be(array: FloatArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_be(0, array) })
suspend fun AsyncOutputStream.writeDoubleArray_be(array: DoubleArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_be(0, array) })

suspend fun AsyncInputStream.readUntil(endByte: Byte, limit: Int = 0x1000): ByteArray {
	val temp = ByteArray(1)
	val out = ByteArrayBuilderSmall()
	try {
		while (true) {
			val c = run { readExact(temp, 0, 1); temp[0] }
			//val c = readS8().toByte()
			if (c == endByte) break
			out.append(c)
			if (out.size >= limit) break
		}
	} catch (e: EOFException) {
	}
	//println("AsyncInputStream.readUntil: '${out.toString(UTF8).replace('\r', ';').replace('\n', '.')}'")
	return out.toByteArray()
}

suspend fun AsyncInputStream.readLine(eol: Char = '\n', charset: Charset = Charsets.UTF_8): String {
	val temp = ByteArray(1)
	val out = ByteArrayBuilderSmall()
	try {
		while (true) {
			val c = run { readExact(temp, 0, 1); temp[0] }
			//val c = readS8().toByte()
			if (c.toChar() == eol) break
			out.append(c.toByte())
		}
	} catch (e: EOFException) {
	}
	return out.toByteArray().toString(charset)
}


fun SyncInputStream.toAsyncInputStream() = object : AsyncInputStream {
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int =
		this@toAsyncInputStream.read(buffer, offset, len)

	suspend override fun close() {
		(this@toAsyncInputStream as? Closeable)?.close()
	}
}

fun SyncOutputStream.toAsyncOutputStream() = object : AsyncOutputStream {
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int) {
		this@toAsyncOutputStream.write(buffer, offset, len)
	}

	suspend override fun close() {
		(this@toAsyncOutputStream as? Closeable)?.close()
	}
}
