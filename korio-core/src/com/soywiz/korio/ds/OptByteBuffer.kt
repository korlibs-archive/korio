package com.soywiz.korio.ds

import com.soywiz.korio.typedarray.copyRangeTo

class OptByteBuffer {
	var size: Int = 0; private set
	val chunks = arrayListOf<ByteArray>()

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

	fun append(chunk: ByteArray, offset: Int, length: Int) {
		val achunk = chunk.copyOfRange(offset, offset + length)
		chunks += achunk
		size += achunk.size
	}

	fun append(buffer: OptByteBuffer) {
		for (c in buffer.chunks) append(c)
	}

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
}