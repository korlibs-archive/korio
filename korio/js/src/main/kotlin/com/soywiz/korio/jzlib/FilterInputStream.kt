package com.soywiz.korio.jzlib

open class FilterInputStream protected constructor(protected var `in`: InputStream) : InputStream() {
	override fun read(): Int = `in`.read()
	override fun read(b: ByteArray): Int = read(b, 0, b.size)
	override fun read(b: ByteArray, off: Int, len: Int): Int = `in`.read(b, off, len)
	override fun skip(n: Long): Long = `in`.skip(n)
	override fun available(): Int = `in`.available()
	override fun close() = run { `in`.close() }
	override fun mark(readlimit: Int) = run { `in`.mark(readlimit) }
	override fun reset() = run { `in`.reset() }
	override fun markSupported(): Boolean = `in`.markSupported()
}
