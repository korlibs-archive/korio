package com.soywiz.korio.util

import java.util.*

class ByteArrayBuffer(var data: ByteArray = ByteArray(0), size: Int = data.size) {
	private var _size: Int = size
	var size: Int
		get() = _size
		set(len) {
			ensure(len)
			_size = len
		}

	fun ensure(expected: Int) {
		if (data.size < expected) {
			data = Arrays.copyOf(data, Math.max(expected, (data.size + 7) * 2))
		}
		_size = Math.max(size, expected)
	}

	fun toByteArraySlice(position: Long = 0) = ByteArraySlice(data, position.toInt(), size)
	fun toByteArray(): ByteArray = Arrays.copyOf(data, size)
}