package com.soywiz.korio.util

import java.util.*

class Pool<T>(val reset: (T) -> Unit = {}, val gen: () -> T) {
	constructor(gen: () -> T) : this({}, gen)

	val items = LinkedList<T>()

	fun obtain(): T {
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

	fun free(v: List<T>) {

		for (it in v) reset(it)
		items.addAll(v)
	}

	inline fun <T2> temp(crossinline callback: (T) -> T2): T2 {
		val temp = obtain()
		try {
			return callback(temp)
		} finally {
			free(temp)
		}
	}
}