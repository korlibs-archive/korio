package com.soywiz.korio.async

import com.soywiz.korio.concurrent.atomic.*

class AsyncPool<T>(val maxItems: Int = Int.MAX_VALUE, val create: suspend () -> T) {
	var createdItems = korAtomic(0)
	private val freedItem = ProduceConsumer<T>()

	suspend fun <TR> tempAlloc(callback: suspend (T) -> TR): TR {
		val item = alloc()
		try {
			return callback(item)
		} finally {
			free(item)
		}
	}

	suspend fun alloc(): T {
		return if (createdItems.value >= maxItems) {
			freedItem.consume()!!
		} else {
			createdItems.addAndGet(1)
			create()
		}
	}

	fun free(item: T) {
		freedItem.produce(item)
	}
}