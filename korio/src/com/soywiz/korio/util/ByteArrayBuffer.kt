package com.soywiz.korio.util

import java.util.*

class ByteArrayBuffer(var data: ByteArray = ByteArray(0)) {
	private var _size: Int = 0

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

	fun toByteArraySlice() = ByteArraySlice(data, 0, size)
	fun toByteArray(): ByteArray = Arrays.copyOf(data, size)
}