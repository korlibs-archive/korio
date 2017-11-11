package com.soywiz.korio.stream

import com.soywiz.kmem.arraycopy
import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.toString
import kotlin.math.max

//@Deprecated("", replaceWith = ReplaceWith("ByteArrayBuilder"))
//typealias OptByteBuffer = ByteArrayBuilder

class ByteArrayBuilder {
	private val chunks = arrayListOf<ByteArray>()

	var size: Int = 0; private set

	constructor() {
	}

	constructor(chunk: ByteArray) {
		append(chunk)
	}

	constructor(chunks: Iterable<ByteArray>) {
		for (chunk in chunks) append(chunk)
	}

	fun append(chunk: ByteArray) {
		chunks += chunk
		size += chunk.size
	}

	fun clear() {
		chunks.clear()
		size = 0
	}

	// @TODO: Optimize this. Maybe storing ranges instead of full arrays?
	fun append(chunk: ByteArray, offset: Int, length: Int) {
		val achunk = chunk.copyOfRange(offset, offset + length)
		chunks += achunk
		size += achunk.size
	}

	fun append(buffer: ByteArrayBuilder) {
		for (c in buffer.chunks) append(c)
	}

	// @TODO: Optimize this. Maybe using a temporal array for this + storing ranges in arrays?
	fun append(v: Byte) {
		append(byteArrayOf(v))
	}

	fun toByteArray(): ByteArray {
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
}

class ByteArrayBuilderSmall(private var bytes: ByteArray, private var len: Int = 0) {
	constructor(capacity: Int = 64) : this(ByteArray(capacity))

	val size: Int get() = len

	fun ensure(size: Int) {
		if (len + size >= bytes.size) {
			bytes = bytes.copyOf(max(bytes.size + size, bytes.size * 2))
		}
	}

	fun append(v: Byte) {
		ensure(1)
		bytes[len++] = v
	}

	fun toByteArray() = bytes.copyOf(len)

	// @TODO: Optimize this!
	fun toString(charset: Charset): String = toByteArray().toString(charset)
}