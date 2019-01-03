package com.soywiz.korio.stream

import com.soywiz.kmem.*
import com.soywiz.korio.lang.*
import kotlin.math.*

//@Deprecated("", replaceWith = ReplaceWith("ByteArrayBuilder"))
//typealias OptByteBuffer = ByteArrayBuilder

internal class ByteArrayBuilder2() {
	private val chunks = arrayListOf<ByteArray>()
	private val small = Small()

	val size get() = chunks.sumBy { it.size } + small.size

	constructor(chunk: ByteArray) : this() {
		append(chunk)
	}

	constructor(vararg chunks: ByteArray) : this() {
		for (chunk in chunks) append(chunk)
	}

	constructor(chunks: Iterable<ByteArray>) : this(*chunks.toList().toTypedArray())

	private fun flush() {
		if (small.size <= 0) return
		chunks += small.toByteArray()
		small.clear()
	}

	fun clear() {
		chunks.clear()
		small.clear()
	}

	fun append(chunk: ByteArray, offset: Int, length: Int) {
		flush()
		val achunk = chunk.copyOfRange(offset, offset + length)
		chunks += achunk
	}

	fun append(buffer: ByteArrayBuilder2) {
		flush()
		chunks += buffer.chunks
	}

	fun append(chunk: ByteArray) = append(chunk, 0, chunk.size)
	fun append(v: Byte) = small.append(v)

	operator fun plusAssign(v: ByteArrayBuilder2) = append(v)
	operator fun plusAssign(v: ByteArray) = append(v)
	operator fun plusAssign(v: Byte) = append(v)

	fun toByteArray(): ByteArray {
		flush()
		val out = ByteArray(size)
		var offset = 0
		for (chunk in chunks) {
			arraycopy(chunk, 0, out, offset, chunk.size)
			offset += chunk.size
		}
		return out
	}

	// @TODO: Optimize this!
	fun toString(charset: Charset): String = toByteArray().toString(charset)

	class Small(var _bytes: ByteArray, var _len: Int = 0) {
		constructor(capacity: Int = 64) : this(ByteArray(capacity))

		val size: Int get() = _len

		fun ensure(size: Int) {
			if (_len + size < _bytes.size) return
			_bytes = _bytes.copyOf(max(_bytes.size + size, _bytes.size * 2))
		}

		fun append(v: Byte) {
			ensure(1)
			appendUnsafe(v)
		}

		inline fun appendUnsafe(v: Byte) {
			_bytes[_len++] = v
		}

		fun append(data: ByteArray, offset: Int, size: Int) {
			ensure(size)
			arraycopy(data, offset, this._bytes, _len, size)
			_len += size
		}

		fun clear() {
			_len = 0
		}

		fun toByteArray() = _bytes.copyOf(_len)

		// @TODO: Optimize this!
		fun toString(charset: Charset): String = toByteArray().toString(charset)
	}
}

@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korio.stream.ByteArrayBuilder"), level = DeprecationLevel.ERROR)
class ByteArrayBuilderSmall(private var bytes: ByteArray, private var len: Int = 0) {
	@Suppress("unused")
	constructor(capacity: Int = 64) : this(ByteArray(capacity))

	val size: Int get() = len

	fun ensure(size: Int) {
		if (len + size < bytes.size) return
		bytes = bytes.copyOf(max(bytes.size + size, bytes.size * 2))
	}

	fun append(v: Byte) {
		ensure(1)
		bytes[len++] = v
	}

	fun clear() {
		len = 0
	}

	fun toByteArray() = bytes.copyOf(len)

	// @TODO: Optimize this!
	fun toString(charset: Charset): String = toByteArray().toString(charset)
}