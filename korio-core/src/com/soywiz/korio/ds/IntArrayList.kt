package com.soywiz.korio.ds

class IntArrayList(capacity: Int = 7) : Collection<Int> {
	var data: IntArray = IntArray(capacity); private set
	internal val capacity: Int get() = data.size
	var length: Int = 0; private set
	override val size: Int get() = length

	constructor(other: IntArrayList) : this() {
		add(other)
	}

	private fun ensure(count: Int) {
		if (length + count > data.size) {
			data = data.copyOf(Math.max(length + count, data.size * 3))
		}
	}

	fun clear() {
		length = 0
	}

	fun add(value: Int) {
		ensure(1)
		data[length++] = value
	}

	operator fun plusAssign(value: Int) = add(value)
	operator fun plusAssign(value: IntArray) = add(value)
	operator fun plusAssign(value: IntArrayList) = add(value)

	fun add(values: IntArray, offset: Int = 0, length: Int = values.size) {
		ensure(values.size)
		System.arraycopy(values, offset, data, this.length, length)
		this.length += values.size
	}

	fun add(values: IntArrayList) = add(values.data, 0, values.length)

	operator fun get(index: Int) = data[index]
	operator fun set(index: Int, value: Int) = run { data[index] = value }

	override fun iterator(): Iterator<Int> = data.take(length).iterator()

	override fun contains(element: Int): Boolean {
		for (n in 0 until length) if (this.data[n] == element) return true
		return false
	}

	override fun containsAll(elements: Collection<Int>): Boolean {
		for (e in elements) if (!contains(e)) return false;
		return true;
	}

	@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
	override fun isEmpty(): Boolean = this.size == 0
}

fun IntArrayList.binarySearch(value: Int) = data.binarySearch(value, 0, length)