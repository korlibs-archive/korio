package com.soywiz.korio.ds

import java.util.*

class DoubleArrayList(capacity: Int = 7) : Collection<Double> {
	var data: DoubleArray = DoubleArray(capacity); private set
	internal val capacity: Int get() = data.size
	var length: Int = 0; private set
	override val size: Int get() = length

	constructor(other: DoubleArrayList) : this() {
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

	fun add(value: Double) {
		ensure(1)
		data[length++] = value
	}

	operator fun plusAssign(value: Double) = add(value)
	operator fun plusAssign(value: DoubleArray) = add(value)
	operator fun plusAssign(value: DoubleArrayList) = add(value)

	fun add(values: DoubleArray, offset: Int = 0, length: Int = values.size) {
		ensure(values.size)
		System.arraycopy(values, offset, data, this.length, length)
		this.length += values.size
	}

	fun add(values: DoubleArrayList) = add(values.data, 0, values.length)

	operator fun get(index: Int) = data[index]
	operator fun set(index: Int, value: Double) = run { data[index] = value }

	override fun iterator(): Iterator<Double> = data.take(length).iterator()

	override fun contains(element: Double): Boolean {
		for (n in 0 until length) if (this.data[n] == element) return true
		return false
	}

	override fun containsAll(elements: Collection<Double>): Boolean {
		for (e in elements) if (!contains(e)) return false;
		return true;
	}

	@Suppress("ReplaceSizeZeroCheckWithIsEmpty")
	override fun isEmpty(): Boolean = this.size == 0
}

fun DoubleArrayList.binarySearch(value: Double) = data.binarySearch(value, 0, length)