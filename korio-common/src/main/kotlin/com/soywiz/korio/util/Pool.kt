package com.soywiz.korio.util

import com.soywiz.korio.ds.LinkedList

class Pool<T>(private val reset: (T) -> Unit = {}, preallocate: Int = 0, private val gen: () -> T) {
	constructor(preallocate: Int = 0, gen: () -> T) : this({}, preallocate, gen)

	private val items = LinkedList<T>()

	val itemsInPool: Int get() = items.size

	init {
		for (n in 0 until preallocate) items += gen()
	}

	fun alloc(): T {
		if (items.isNotEmpty()) {
			return items.removeLast()
		} else {
			return gen()
		}
	}

	fun free(v: T) {
		reset(v)
		items.addFirst(v)
	}

	fun free(v: Iterable<T>) {
		for (it in v) reset(it)
		items.addAll(v)
	}

	inline fun <T2> alloc(crossinline callback: (T) -> T2): T2 {
		val temp = alloc()
		try {
			return callback(temp)
		} finally {
			free(temp)
		}
	}
}