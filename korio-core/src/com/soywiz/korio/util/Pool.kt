package com.soywiz.korio.util

import java.util.*

class Pool<T>(val gen: () -> T) {
	val items = LinkedList<T>()

	fun obtain(): T {
		if (items.isNotEmpty()) {
			return items.removeLast()
		} else {
			return gen()
		}
	}

	fun free(v: T) {
		items.addFirst(v)
	}

	fun free(v: List<T>) {
		items.addAll(v)
	}
}