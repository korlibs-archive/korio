package com.soywiz.coktvfs

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

class SliceSyncStream(val base: SyncStream, val baseOffset: Long, val baseLength: Long) : SyncStream() {
    override var position: Long = 0L
    override var length: Long = baseLength

    override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
        return base.keepPosition {
            base.position = this.baseOffset + position
            val res = base.read(buffer, offset, len)
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
}

class MemorySyncStream(var data: ByteArray) : SyncStream() {
    override var position: Long = 0L
    override var length: Long
        get() = data.size.toLong()
        set(value) {
            if (value != data.size.toLong()) data = Arrays.copyOf(data, value.toInt())
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
}

fun SyncStream.slice(position: Long, length: Long): SyncStream {
    return SliceSyncStream(this, position, length)
}

fun SyncStream.readSlice(length: Long): SyncStream {
    val out = SliceSyncStream(this, position, length)
    position += length
    return out
}

fun SyncStream.readStringz(len: Int, charset: Charset = Charsets.UTF_8): String {
    val res = readBytes(len)
    val index = res.indexOf(0.toByte())
    return String(res, 0, if (index < 0) len else index, charset)
}

fun SyncStream.readString(len: Int, charset: Charset = Charsets.UTF_8): String = readBytes(len).toString(charset)
fun SyncStream.writeString(string: String, charset: Charset = Charsets.UTF_8): Unit = writeBytes(string.toByteArray(charset))

fun SyncStream.readBytes(len: Int): ByteArray = ByteArray(len).apply { read(this, 0, len) }
fun SyncStream.writeBytes(data: ByteArray): Unit = write(data, 0, data.size)

fun SyncStream.readU8(): Int = temp.apply { read(temp, 0, 1) }.readU8(0)

fun SyncStream.readU16_le(): Int = temp.apply { read(temp, 0, 2) }.readU16_le(0)
fun SyncStream.readU32_le(): Long = temp.apply { read(temp, 0, 4) }.readU32_le(0)

fun SyncStream.readS16_le(): Int = temp.apply { read(temp, 0, 2) }.readS16_le(0)
fun SyncStream.readS32_le(): Int = temp.apply { read(temp, 0, 4) }.readS32_le(0)
fun SyncStream.readS64_le(): Long = temp.apply { read(temp, 0, 8) }.readS64_le(0)

fun SyncStream.readU16_be(): Int = temp.apply { read(temp, 0, 2) }.readU16_be(0)
fun SyncStream.readU32_be(): Long = temp.apply { read(temp, 0, 4) }.readU32_be(0)

fun SyncStream.readS16_be(): Int = temp.apply { read(temp, 0, 2) }.readS16_be(0)
fun SyncStream.readS32_be(): Int = temp.apply { read(temp, 0, 4) }.readS32_be(0)
fun SyncStream.readS64_be(): Long = temp.apply { read(temp, 0, 8) }.readS64_be(0)

fun SyncStream.readAvailable(): ByteArray = readBytes(available.toInt())

fun ByteArray.open(): SyncStream = MemorySyncStream(this)