package com.soywiz.korio.ds

class IntArrayList(capacity: Int = 7) : Iterable<Int> {
	var data: IntArray = IntArray(capacity); private set
	internal val capacity: Int get() = data.size
	var length: Int = 0; private set
	val size: Int get() = length

	private fun ensure(count: Int) {
		if (length + count > data.size) {
			data = data.copyOf(Math.max(length + count, data.size * 3))
		}
	}

	fun add(value: Int) {
		ensure(1)
		data[length++] = value
	}

	fun add(values: IntArray) {
		ensure(values.size)
		System.arraycopy(values, 0, data, length, values.size)
		length += values.size
	}

	operator fun get(index: Int) = data[index]
	operator fun set(index: Int, value: Int) = run { data[index] = value }

	override fun iterator(): Iterator<Int> = data.take(length).iterator()
}

fun IntArrayList.binarySearch(value: Int) = data.binarySearch(value, 0, length)