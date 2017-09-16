package com.soywiz.korio.ds

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

	fun append(buffer: OptByteBuffer) {
		for (c in buffer.chunks) append(c)
	}

	fun toByteArray(): ByteArray {
		val out = ByteArray(size)
		var offset = 0
		for (chunk in chunks) {
			System.arraycopy(chunk, 0, out, offset, chunk.size)
			offset += chunk.size
		}
		return out
	}
}