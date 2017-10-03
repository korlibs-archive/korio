package com.soywiz.korio.ds

import com.soywiz.korio.lang.Charset
import com.soywiz.korio.lang.toString
import com.soywiz.korio.typedarray.copyRangeTo

typealias OptByteBuffer = ByteArrayBuilder

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
			chunk.copyRangeTo(0, out, offset, chunk.size)
			offset += chunk.size
		}
		return out
	}

	fun toString(charset: Charset): String = toByteArray().toString(charset)
}