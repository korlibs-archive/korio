package com.soywiz.korio.stream

import com.soywiz.korio.*
import java.io.*

class FileSyncStreamBase(val file: java.io.File, val mode: String = "r") : SyncStreamBase() {
	val ra = RandomAccessFile(file, mode)

	override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = synchronized2(ra) {
		ra.seek(position)
		return ra.read(buffer, offset, len)
	}

	override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = synchronized2(ra) {
		ra.seek(position)
		ra.write(buffer, offset, len)
	}

	override var length: Long
		get() = ra.length()
		set(value) = run { ra.setLength(value) }

	override fun close() = ra.close()
}

fun File.openSync(mode: String = "r"): SyncStream = FileSyncStreamBase(this, mode).toSyncStream()

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
