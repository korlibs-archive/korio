package com.soywiz.korio.stream

import com.soywiz.korio.util.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.*

open class SyncStream {
	open fun read(buffer: ByteArray, offset: Int, len: Int): Int = throw UnsupportedOperationException()
	open fun write(buffer: ByteArray, offset: Int, len: Int): Unit = throw UnsupportedOperationException()
	open var position: Long
		set(value) = throw UnsupportedOperationException()
		get() = run { throw UnsupportedOperationException() }
	open var length: Long
		set(value) = throw UnsupportedOperationException()
		get() = run { throw UnsupportedOperationException() }
	val available: Long get() = length - position
	internal val temp = ByteArray(16)
}

inline fun <T> SyncStream.keepPosition(callback: () -> T): T {
	val old = this.position
	try {
		return callback()
	} finally {
		this.position = old
	}
}

class SliceSyncStream(internal val base: SyncStream, internal val baseOffset: Long, internal val baseEnd: Long) : SyncStream() {
	internal val baseLength: Long = baseEnd - baseOffset

	override var position: Long = 0L
	override var length: Long = baseLength

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		return base.keepPosition {
			base.position = this.baseOffset + position
			val rlen = Math.min(available, len.toLong()).toInt()
			val res = if (rlen > 0) base.read(buffer, offset, rlen) else 0
			position += res
			res
		}
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int) {
		return base.keepPosition {
			base.position = this.baseOffset + position
			base.write(buffer, offset, len)
			position += len
		}
	}

	override fun toString(): String = "SliceSyncStream($base, $baseOffset, $baseEnd)"
}

class FileSyncStream(val file: File, val mode: String = "r") : SyncStream() {
	val ra = RandomAccessFile(file, mode)

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		return ra.read(buffer, offset, len)
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int) {
		ra.write(buffer, offset, len)
	}

	override var position: Long
		get() = ra.filePointer
		set(value) {
			ra.seek(value)
		}
	override var length: Long
		get() = ra.length()
		set(value) {
			ra.setLength(value)
		}
}

class MemorySyncStream(var data: ByteArray = ByteArray(0)) : SyncStream() {
	override var position: Long = 0L
	override var length: Long = data.size.toLong()
		set(value) {
			if (value > data.size.toLong()) {
				data = Arrays.copyOf(data, Math.max(value.toInt(), (data.size + 7) * 2))
			}
			field = value
		}

	override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		val read = Math.min(len, available.toInt())
		System.arraycopy(this.data, this.position.toInt(), buffer, offset, read)
		this.position += read
		return read
	}

	override fun write(buffer: ByteArray, offset: Int, len: Int) {
		this.length = Math.max(this.position + len, this.length)
		System.arraycopy(buffer, offset, this.data, this.position.toInt(), len)
		this.position += len
	}

	fun toByteArraySlice() = ByteArraySlice(data, 0, length.toInt())
	fun toByteArray(): ByteArray = Arrays.copyOf(data, length.toInt())
	override fun toString(): String = "MemorySyncStream(${data.size})"
}

fun SyncStream.sliceWithStart(start: Long): SyncStream = sliceWithBounds(start, this.length)

fun SyncStream.slice(): SyncStream = SliceSyncStream(this, 0L, length)

fun SyncStream.slice(range: IntRange): SyncStream = sliceWithBounds(range.start.toLong(), (range.endInclusive.toLong() + 1))
fun SyncStream.slice(range: LongRange): SyncStream = sliceWithBounds(range.start, (range.endInclusive + 1))

fun SyncStream.sliceWithBounds(start: Long, end: Long): SyncStream = SliceSyncStream(this, start, end)
fun SyncStream.sliceWithSize(position: Long, length: Long): SyncStream = sliceWithBounds(position, position + length)

fun SyncStream.readSlice(length: Long): SyncStream = sliceWithSize(position, length).apply {
	this@readSlice.position += length
}

fun SyncStream.readStream(length: Int): SyncStream = readSlice(length.toLong())
fun SyncStream.readStream(length: Long): SyncStream = readSlice(length)

fun SyncStream.readStringz(charset: Charset = Charsets.UTF_8): String {
	val buf = ByteArrayOutputStream()
	while (!eof) {
		val b = readU8()
		if (b == 0) break
		buf.write(b.toInt())
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
		if (read <= 0) throw RuntimeException("EOF")
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
private fun SyncStream.readTempExact(count: Int): ByteArray = temp.apply { readExact(temp, 0, count) }

fun SyncStream.readU8(): Int = readTempExact(1).readU8(0)

fun SyncStream.readU16_le(): Int = readTempExact(2).readU16_le(0)
fun SyncStream.readU24_le(): Int = readTempExact(3).readU24_le(0)
fun SyncStream.readU32_le(): Long = readTempExact(4).readU32_le(0)

fun SyncStream.readS16_le(): Int = readTempExact(2).readS16_le(0)
fun SyncStream.readS32_le(): Int = readTempExact(4).readS32_le(0)
fun SyncStream.readS64_le(): Long = readTempExact(8).readS64_le(0)

fun SyncStream.readF32_le(): Float = readTempExact(4).readF32_le(0)
fun SyncStream.readF64_le(): Double = readTempExact(8).readF64_le(0)

fun SyncStream.readU16_be(): Int = readTempExact(2).readU16_be(0)
fun SyncStream.readU24_be(): Int = readTempExact(3).readU24_be(0)
fun SyncStream.readU32_be(): Long = readTempExact(4).readU32_be(0)

fun SyncStream.readS16_be(): Int = readTempExact(2).readS16_be(0)
fun SyncStream.readS32_be(): Int = readTempExact(4).readS32_be(0)
fun SyncStream.readS64_be(): Long = readTempExact(8).readS64_be(0)

fun SyncStream.readF32_be(): Float = readTempExact(4).readF32_be(0)
fun SyncStream.readF64_be(): Double = readTempExact(8).readF64_be(0)

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

fun SyncStream.write8(v: Int): Unit = write(temp.apply { write8(0, v) }, 0, 1)

fun SyncStream.write16_le(v: Int): Unit = write(temp.apply { write16_le(0, v) }, 0, 2)
fun SyncStream.write24_le(v: Int): Unit = write(temp.apply { write24_le(0, v) }, 0, 3)
fun SyncStream.write32_le(v: Int): Unit = write(temp.apply { write32_le(0, v) }, 0, 4)
fun SyncStream.write32_le(v: Long): Unit = write(temp.apply { write32_le(0, v) }, 0, 4)
fun SyncStream.write64_le(v: Long): Unit = write(temp.apply { write64_le(0, v) }, 0, 8)
fun SyncStream.writeF32_le(v: Float): Unit = write(temp.apply { writeF32_le(0, v) }, 0, 4)
fun SyncStream.writeF64_le(v: Double): Unit = write(temp.apply { writeF64_le(0, v) }, 0, 8)

fun SyncStream.write16_be(v: Int): Unit = write(temp.apply { write16_be(0, v) }, 0, 2)
fun SyncStream.write24_be(v: Int): Unit = write(temp.apply { write24_be(0, v) }, 0, 3)
fun SyncStream.write32_be(v: Int): Unit = write(temp.apply { write32_be(0, v) }, 0, 4)
fun SyncStream.write32_be(v: Long): Unit = write(temp.apply { write32_be(0, v) }, 0, 4)
fun SyncStream.write64_be(v: Long): Unit = write(temp.apply { write64_be(0, v) }, 0, 8)
fun SyncStream.writeF32_be(v: Float): Unit = write(temp.apply { writeF32_be(0, v) }, 0, 4)
fun SyncStream.writeF64_be(v: Double): Unit = write(temp.apply { writeF64_be(0, v) }, 0, 8)

fun ByteArray.openSync(mode: String = "r"): MemorySyncStream = MemorySyncStream(this)
fun ByteArray.openAsync(mode: String = "r") = openSync(mode).toAsync()
fun File.openSync(mode: String = "r"): FileSyncStream = FileSyncStream(this, mode)

fun SyncStream.writeStream(source: SyncStream): Unit = source.copyTo(this)

fun SyncStream.copyTo(target: SyncStream): Unit {
	val chunk = BYTES_TEMP
	while (true) {
		val count = this.read(chunk)
		if (count <= 0) break
		target.write(chunk, 0, count)
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
	while ((position % alignment) != 0L) {
		write8(value)
	}
}

fun SyncStream.skipToAlign(alignment: Int) {
	while ((position % alignment) != 0L) {
		readU8()
	}
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
