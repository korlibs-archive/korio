package com.soywiz.korio.stream

import com.soywiz.korio.util.*
import org.omg.IOP.Encoding
import java.io.*
import java.nio.charset.Charset
import java.util.*

interface SyncInputStream {
	fun read(buffer: ByteArray, offset: Int, len: Int): Int
}

interface SyncOutputStream {
	fun write(buffer: ByteArray, offset: Int, len: Int): Unit
}

interface SyncLengthStream {
	var length: Long
}

interface SyncRAInputStream {
	fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int
}

interface SyncRAOutputStream {
	fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit
}

open class SyncStreamBase : Closeable, SyncRAInputStream, SyncRAOutputStream, SyncLengthStream {
	val smallTemp = ByteArray(16)
	fun read(position: Long): Int {
		val count = read(position, smallTemp, 0, 1)
		return if (count >= 1) smallTemp[0].toInt() and 0xFF else -1
	}
	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = throw UnsupportedOperationException()
	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = throw UnsupportedOperationException()
	override var length: Long
		set(value) = throw UnsupportedOperationException()
		get() = throw UnsupportedOperationException()

	override fun close() = Unit
}


class SyncStream(val base: SyncStreamBase, var position: Long = 0L) : Extra by Extra.Mixin(), Closeable, SyncInputStream, SyncOutputStream, SyncLengthStream {
	val smallTemp = ByteArray(16)

	fun read(): Int {
		val count = read(smallTemp, 0, 1)
		return if (count < 0) -1 else smallTemp[0].toInt() and 0xFF
	}

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		val read = base.read(position, buffer, offset, len)
		position += read
		return read
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int): Unit {
		base.write(position, buffer, offset, len)
		position += len
	}

	override var length: Long
		set(value) = run { base.length = value }
		get() = base.length

	val available: Long get() = length - position

	override fun close(): Unit = base.close()

	fun clone() = SyncStream(base, position)

	override fun toString(): String = "SyncStream($base, $position)"
}

inline fun <T> SyncStream.keepPosition(callback: () -> T): T {
	val old = this.position
	try {
		return callback()
	} finally {
		this.position = old
	}
}

class SliceSyncStreamBase(internal val base: SyncStreamBase, internal val baseStart: Long, internal val baseEnd: Long) : SyncStreamBase() {
	internal val baseLength: Long = baseEnd - baseStart

	override var length: Long
		set(value) = throw UnsupportedOperationException()
		get() = baseLength

	private fun clampPosition(position: Long) = position.clamp(baseStart, baseEnd)

	private fun clampPositionLen(position: Long, len: Int): Pair<Long, Int> {
		if (position < 0L) throw IllegalArgumentException("Invalid position")
		val targetStartPosition = clampPosition(this.baseStart + position)
		val targetEndPosition = clampPosition(targetStartPosition + len)
		val targetLen = (targetEndPosition - targetStartPosition).toInt()
		return Pair(targetStartPosition, targetLen)
	}

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.read(targetStartPosition, buffer, offset, targetLen)
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		val (targetStartPosition, targetLen) = clampPositionLen(position, len)
		return base.write(targetStartPosition, buffer, offset, targetLen)
	}

	override fun close() = Unit

	override fun toString(): String = "SliceAsyncStreamBase($base, $baseStart, $baseEnd)"
}

class FileSyncStreamBase(val file: File, val mode: String = "r") : SyncStreamBase() {
	val ra = RandomAccessFile(file, mode)

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = synchronized(ra) {
		ra.seek(position)
		return ra.read(buffer, offset, len)
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = synchronized(ra) {
		ra.seek(position)
		ra.write(buffer, offset, len)
	}

	override var length: Long
		get() = ra.length()
		set(value) = run { ra.setLength(value) }

	override fun close() = ra.close()
}

class FillSyncStreamBase(val fill: Byte, override var length: Long) : SyncStreamBase() {
	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val end = Math.min(length, position + len)
		val actualLen = (end - position).toIntSafe()
		Arrays.fill(buffer, offset, offset + actualLen, fill)
		return actualLen
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = Unit

	override fun close() = Unit
}

fun FillSyncStream(fillByte: Int = 0, length: Long = Long.MAX_VALUE) = FillSyncStreamBase(fillByte.toByte(), length).toSyncStream()

fun MemorySyncStream(data: ByteArray = ByteArray(0)) = MemorySyncStreamBase(ByteArrayBuffer(data)).toSyncStream()
fun MemorySyncStream(data: ByteArrayBuffer) = MemorySyncStreamBase(data).toSyncStream()
inline fun MemorySyncStreamToByteArray(initialCapacity: Int = 4096, callback: SyncStream.() -> Unit): ByteArray {
	val buffer = ByteArrayBuffer(initialCapacity)
	val s = MemorySyncStream(buffer)
	callback(s)
	return buffer.toByteArray()
}

val SyncStream.hasLength: Boolean get() = try {
	length; true
} catch (e: Throwable) {
	false
}
val SyncStream.hasAvailable: Boolean get() = try {
	available; true
} catch (e: Throwable) {
	false
}

fun SyncStream.toByteArray(): ByteArray {
	if (hasLength) {
		return this.sliceWithBounds(0L, length).readAll()
	} else {
		return this.clone().readAll()
	}
}

class MemorySyncStreamBase(var data: ByteArrayBuffer) : SyncStreamBase() {
	constructor(initialCapacity: Int = 4096) : this(ByteArrayBuffer(initialCapacity))

	override var length: Long
		get() = data.size.toLong()
		set(value) = run { data.size = value.toInt() }

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		val end = Math.min(this.length, position + len)
		val actualLen = (end - position).toInt()
		System.arraycopy(this.data.data, position.toInt(), buffer, offset, actualLen)
		return actualLen
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		data.ensure((position + len).toInt())
		System.arraycopy(buffer, offset, this.data.data, position.toInt(), len)
	}

	override fun close() = Unit

	override fun toString(): String = "MemorySyncStreamBase(${data.size})"
}

fun SyncStream.sliceWithStart(start: Long): SyncStream = sliceWithBounds(start, this.length)

fun SyncStream.slice(): SyncStream = SyncStream(SliceSyncStreamBase(this.base, 0L, length))

fun SyncStream.slice(range: IntRange): SyncStream = sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1))
fun SyncStream.slice(range: LongRange): SyncStream = sliceWithBounds(range.start, (range.endInclusive + 1))

fun SyncStream.sliceWithBounds(start: Long, end: Long): SyncStream {
	val len = this.length
	val clampedStart = start.clamp(0, len)
	val clampedEnd = end.clamp(0, len)
	if (this.base is SliceSyncStreamBase) {
		return SliceSyncStreamBase(this.base.base, this.base.baseStart + clampedStart, this.base.baseStart + clampedEnd).toSyncStream()
	} else {
		return SliceSyncStreamBase(this.base, clampedStart, clampedEnd).toSyncStream()
	}
}

fun SyncStream.sliceWithSize(position: Long, length: Long): SyncStream = sliceWithBounds(position, position + length)

fun SyncStream.readSlice(length: Long): SyncStream = sliceWithSize(position, length).apply {
	this@readSlice.position += length
}

fun SyncStream.readStream(length: Int): SyncStream = readSlice(length.toLong())
fun SyncStream.readStream(length: Long): SyncStream = readSlice(length)

fun SyncStream.readStringz(charset: Charset = Charsets.UTF_8): String {
	val buf = ByteArrayOutputStream()
	val temp = BYTES_TEMP
	while (true) {
		val read = read(temp, 0, 1)
		if (read <= 0) break
		if (temp[0] == 0.toByte()) break
		buf.write(temp[0].toInt())
	}
	return buf.toByteArray().toString(charset)
}

fun SyncStream.readStringz(len: Int, charset: Charset = Charsets.UTF_8): String {
	val res = readBytes(len)
	val index = res.indexOf(0.toByte())
	return String(res, 0, if (index < 0) len else index, charset)
}

fun SyncStream.readString(len: Int, charset: Charset = Charsets.UTF_8): String = readBytes(len).toString(charset)
fun SyncStream.writeString(string: String, charset: Charset = Charsets.UTF_8): Unit = writeBytes(string.toByteArray(charset))

fun SyncStream.readExact(out: ByteArray, offset: Int, len: Int): Unit {
	var ooffset = offset
	var remaining = len
	while (remaining > 0) {
		val read = read(out, ooffset, remaining)
		if (read <= 0) {
			throw RuntimeException("EOF")
		}
		remaining -= read
		ooffset += read
	}
}

fun SyncStream.read(data: ByteArray): Int = read(data, 0, data.size)
fun SyncStream.read(data: UByteArray): Int = read(data.data, 0, data.size)

fun SyncStream.readBytesExact(len: Int): ByteArray = ByteArray(len).apply { readExact(this, 0, len) }

fun SyncStream.writeStringz(str: String, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(charset))
fun SyncStream.writeStringz(str: String, len: Int, charset: Charset = Charsets.UTF_8) = this.writeBytes(str.toBytez(len, charset))

fun SyncStream.readBytes(len: Int): ByteArray {
	val bytes = ByteArray(len)
	return Arrays.copyOf(bytes, read(bytes, 0, len))
}

fun SyncStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)
fun SyncStream.writeBytes(data: ByteArraySlice): Unit = write(data.data, data.position, data.length)

val SyncStream.eof: Boolean get () = this.available <= 0L
private fun SyncStream.readSmallTempExact(count: Int): ByteArray {
	val t = this.smallTemp
	readExact(t, 0, count)
	return t
}

private fun SyncStream.readTempExact(count: Int): ByteArray {
	val temp = BYTES_TEMP
	return temp.apply { readExact(temp, 0, count) }
}

private fun SyncStream.readTemp(count: Int): ByteArray {
	val temp = BYTES_TEMP
	return temp.apply { read(temp, 0, count) }
}

fun SyncStream.fastReadU8(): Int = this.read() and 0xFF
fun SyncStream.fastReadS8(): Int = this.read().toByte().toInt()

fun SyncStream.readU8(): Int = readSmallTempExact(1).readU8(0)
fun SyncStream.readS8(): Int = readSmallTempExact(1).readS8(0)

fun SyncStream.readU16_le(): Int = readSmallTempExact(2).readU16_le(0)
fun SyncStream.readU24_le(): Int = readSmallTempExact(3).readU24_le(0)
fun SyncStream.readU32_le(): Long = readSmallTempExact(4).readU32_le(0)

fun SyncStream.readS16_le(): Int = readSmallTempExact(2).readS16_le(0)
fun SyncStream.readS24_le(): Int = readSmallTempExact(3).readS24_le(0)
fun SyncStream.readS32_le(): Int = readSmallTempExact(4).readS32_le(0)
fun SyncStream.readS64_le(): Long = readSmallTempExact(8).readS64_le(0)

fun SyncStream.readF32_le(): Float = readSmallTempExact(4).readF32_le(0)
fun SyncStream.readF64_le(): Double = readSmallTempExact(8).readF64_le(0)

fun SyncStream.readU16_be(): Int = readSmallTempExact(2).readU16_be(0)
fun SyncStream.readU24_be(): Int = readSmallTempExact(3).readU24_be(0)
fun SyncStream.readU32_be(): Long = readSmallTempExact(4).readU32_be(0)

fun SyncStream.readS16_be(): Int = readSmallTempExact(2).readS16_be(0)
fun SyncStream.readS24_be(): Int = readSmallTempExact(3).readS24_be(0)
fun SyncStream.readS32_be(): Int = readSmallTempExact(4).readS32_be(0)
fun SyncStream.readS64_be(): Long = readSmallTempExact(8).readS64_be(0)

fun SyncStream.readF32_be(): Float = readSmallTempExact(4).readF32_be(0)
fun SyncStream.readF64_be(): Double = readSmallTempExact(8).readF64_be(0)

fun SyncStream.readAvailable(): ByteArray = readBytes(available.toInt())
fun SyncStream.readAll(): ByteArray = readBytes(available.toInt())

fun SyncStream.readUByteArray(count: Int): UByteArray = UByteArray(readBytesExact(count))

fun SyncStream.readShortArray_le(count: Int): ShortArray = readBytesExact(count * 2).readShortArray_le(0, count)
fun SyncStream.readShortArray_be(count: Int): ShortArray = readBytesExact(count * 2).readShortArray_be(0, count)

fun SyncStream.readCharArray_le(count: Int): CharArray = readBytesExact(count * 2).readCharArray_le(0, count)
fun SyncStream.readCharArray_be(count: Int): CharArray = readBytesExact(count * 2).readCharArray_be(0, count)

fun SyncStream.readIntArray_le(count: Int): IntArray = readBytesExact(count * 4).readIntArray_le(0, count)
fun SyncStream.readIntArray_be(count: Int): IntArray = readBytesExact(count * 4).readIntArray_be(0, count)

fun SyncStream.readLongArray_le(count: Int): LongArray = readBytesExact(count * 8).readLongArray_le(0, count)
fun SyncStream.readLongArray_be(count: Int): LongArray = readBytesExact(count * 8).readLongArray_be(0, count)

fun SyncStream.readFloatArray_le(count: Int): FloatArray = readBytesExact(count * 4).readFloatArray_le(0, count)
fun SyncStream.readFloatArray_be(count: Int): FloatArray = readBytesExact(count * 4).readFloatArray_be(0, count)

fun SyncStream.readDoubleArray_le(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArray_le(0, count)
fun SyncStream.readDoubleArray_be(count: Int): DoubleArray = readBytesExact(count * 8).readDoubleArray_be(0, count)

fun SyncStream.write8(v: Int): Unit = write(BYTES_TEMP.apply { write8(0, v) }, 0, 1)

fun SyncStream.write16_le(v: Int): Unit = write(BYTES_TEMP.apply { write16_le(0, v) }, 0, 2)
fun SyncStream.write24_le(v: Int): Unit = write(BYTES_TEMP.apply { write24_le(0, v) }, 0, 3)
fun SyncStream.write32_le(v: Int): Unit = write(BYTES_TEMP.apply { write32_le(0, v) }, 0, 4)
fun SyncStream.write32_le(v: Long): Unit = write(BYTES_TEMP.apply { write32_le(0, v) }, 0, 4)
fun SyncStream.write64_le(v: Long): Unit = write(BYTES_TEMP.apply { write64_le(0, v) }, 0, 8)
fun SyncStream.writeF32_le(v: Float): Unit = write(BYTES_TEMP.apply { writeF32_le(0, v) }, 0, 4)
fun SyncStream.writeF64_le(v: Double): Unit = write(BYTES_TEMP.apply { writeF64_le(0, v) }, 0, 8)

fun SyncStream.write16_be(v: Int): Unit = write(BYTES_TEMP.apply { write16_be(0, v) }, 0, 2)
fun SyncStream.write24_be(v: Int): Unit = write(BYTES_TEMP.apply { write24_be(0, v) }, 0, 3)
fun SyncStream.write32_be(v: Int): Unit = write(BYTES_TEMP.apply { write32_be(0, v) }, 0, 4)
fun SyncStream.write32_be(v: Long): Unit = write(BYTES_TEMP.apply { write32_be(0, v) }, 0, 4)
fun SyncStream.write64_be(v: Long): Unit = write(BYTES_TEMP.apply { write64_be(0, v) }, 0, 8)
fun SyncStream.writeF32_be(v: Float): Unit = write(BYTES_TEMP.apply { writeF32_be(0, v) }, 0, 4)
fun SyncStream.writeF64_be(v: Double): Unit = write(BYTES_TEMP.apply { writeF64_be(0, v) }, 0, 8)

fun SyncStreamBase.toSyncStream(position: Long = 0L) = SyncStream(this, position)

fun ByteArray.openSync(mode: String = "r"): SyncStream = MemorySyncStreamBase(ByteArrayBuffer(this)).toSyncStream(0L)
fun ByteArray.openAsync(mode: String = "r"): AsyncStream = openSync(mode).toAsync()
fun String.openAsync(charset: Charset = Charsets.UTF_8): AsyncStream = toByteArray(charset).openSync("r").toAsync()
fun File.openSync(mode: String = "r"): SyncStream = FileSyncStreamBase(this, mode).toSyncStream()

fun SyncStream.writeStream(source: SyncStream): Unit = source.copyTo(this)

fun SyncStream.copyTo(target: SyncStream): Unit {
	val chunk = BYTES_TEMP
	while (true) {
		val count = this.read(chunk)
		if (count <= 0) break
		target.write(chunk, 0, count)
	}
}

fun InputStream.toSyncStream(): SyncInputStream {
	val iss = this
	return object : SyncInputStream {
		override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
			return iss.read(buffer, offset, len)
		}
	}
}

fun SyncStream.toInputStream(): InputStream {
	val ss = this
	return object : InputStream() {
		override fun read(): Int = if (ss.eof) -1 else ss.readU8()
		override fun read(b: ByteArray, off: Int, len: Int): Int = ss.read(b, off, len)
		override fun available(): Int = ss.available.toInt()
	}
}

fun SyncStream.writeToAlign(alignment: Int, value: Int = 0) {
	val nextPosition = position.nextAlignedTo(alignment.toLong())
	val data = ByteArray((nextPosition - position).toInt())
	Arrays.fill(data, value.toByte())
	writeBytes(data)
}

fun SyncStream.skip(count: Int): SyncStream {
	position += count
	return this
}

fun SyncStream.skipToAlign(alignment: Int) {
	val nextPosition = position.nextAlignedTo(alignment.toLong())
	readBytes((nextPosition - position).toInt())
}

fun SyncStream.truncate() = run { length = position }

fun SyncStream.writeCharArray_le(array: CharArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_le(0, array) })
fun SyncStream.writeShortArray_le(array: ShortArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_le(0, array) })
fun SyncStream.writeIntArray_le(array: IntArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_le(0, array) })
fun SyncStream.writeLongArray_le(array: LongArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_le(0, array) })
fun SyncStream.writeFloatArray_le(array: FloatArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_le(0, array) })
fun SyncStream.writeDoubleArray_le(array: DoubleArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_le(0, array) })

fun SyncStream.writeCharArray_be(array: CharArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_be(0, array) })
fun SyncStream.writeShortArray_be(array: ShortArray) = writeBytes(ByteArray(array.size * 2).apply { writeArray_be(0, array) })
fun SyncStream.writeIntArray_be(array: IntArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_be(0, array) })
fun SyncStream.writeLongArray_be(array: LongArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_be(0, array) })
fun SyncStream.writeFloatArray_be(array: FloatArray) = writeBytes(ByteArray(array.size * 4).apply { writeArray_be(0, array) })
fun SyncStream.writeDoubleArray_be(array: DoubleArray) = writeBytes(ByteArray(array.size * 8).apply { writeArray_be(0, array) })

// Variable Length

fun SyncStream.readU_VL(): Int {
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
fun SyncStream.readS_VL(): Int {
	val v = readU_VL()
	val sign = ((v and 1) != 0)
	val uvalue = v ushr 1
	return if (sign) -uvalue - 1 else uvalue
}

fun SyncStream.writeU_VL(v: Int): Unit {
	var value = v
	while (true) {
		val c = value and 0x7f
		value = value ushr 7
		if (value == 0) {
			write8(c)
			break
		}
		write8(c or 0x80)
	}
}
fun SyncStream.writeS_VL(v: Int): Unit {
	val sign = if (v < 0) 1 else 0
	writeU_VL(sign or ((if (v < 0) -v - 1 else v) shl 1))
}

fun SyncStream.writeStringVL(str: String, charset: Charset = Charsets.UTF_8): Unit {
	val bytes = str.toByteArray(charset)
	writeU_VL(bytes.size)
	writeBytes(bytes)
}

fun SyncStream.readStringVL(charset: Charset = Charsets.UTF_8): String {
	val bytes = ByteArray(readU_VL())
	readExact(bytes, 0, bytes.size)
	return bytes.toString(charset)
}

fun InputStream.readExactTo(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size - offset): Int {
	val end = offset + length
	var pos = offset
	while (true) {
		val read = this.read(buffer, pos, end - pos)
		if (read <= 0) break
		pos += read
	}
	return pos - offset
}

//fun InputStream.readBytesFast(expectedSize: Int, buffer: ByteArray): Int {
//}