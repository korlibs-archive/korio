package com.soywiz.korio.stream

import com.soywiz.kmem.arraycopy
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.toString
import kotlin.math.max

//@Deprecated("", replaceWith = ReplaceWith("ByteArrayBuilder"))
//typealias OptByteBuffer = ByteArrayBuilder

class ByteArrayBuilder() {
	private val chunks = arrayListOf<ByteArray>()
	private val small = Small()

	val size get() = chunks.sumBy { it.size } + small.size

	constructor(chunk: ByteArray) : this() {
		append(chunk)
	}

	constructor(chunks: Iterable<ByteArray>) : this() {
		for (chunk in chunks) append(chunk)
	}

	constructor(vararg chunks: ByteArray) : this() {
		for (chunk in chunks) append(chunk)
	}

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

	fun append(buffer: ByteArrayBuilder) {
		flush()
		chunks += buffer.chunks
	}

	fun append(chunk: ByteArray) = append(chunk, 0, chunk.size)
	fun append(v: Byte) = small.append(v)

	operator fun plusAssign(v: ByteArrayBuilder) = append(v)
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

	private class Small(private var bytes: ByteArray, private var len: Int = 0) {
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
}

@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korio.stream.ByteArrayBuilder"))
class ByteArrayBuilderSmall(private var bytes: ByteArray, private var len: Int = 0) {
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