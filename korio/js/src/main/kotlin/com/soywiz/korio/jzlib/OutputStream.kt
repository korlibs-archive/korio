package com.soywiz.korio.jzlib

abstract class OutputStream() {
	open fun close() = Unit
	open fun flush(): Unit = Unit
	abstract fun write(oneByte: Int)
	open fun checkError(): Boolean = false
	open fun write(buffer: ByteArray) = write(buffer, 0, buffer.size)
	open fun write(buffer: ByteArray, offset: Int, count: Int) = run { for (i in offset until offset + count) write(buffer[i].toInt()) }
}

open class FilterOutputStream(protected var out: OutputStream) : OutputStream() {
	override fun write(value: Int) = run { out.write(value) }
	override fun write(value: ByteArray) = run { write(value, 0, value.size) }
	override fun write(value: ByteArray, offset: Int, length: Int) = run { for (n in 0 until length) write(value[offset + n].toInt()) }
	override fun flush() = run { out.flush() }
	override fun close() {
		val ostream = out
		try {
			flush()
		} finally {
			ostream.close()
		}
	}
}
