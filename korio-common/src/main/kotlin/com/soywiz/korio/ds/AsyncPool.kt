package com.soywiz.korio.ds

import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.lang.AtomicInteger

class AsyncPool<T>(val maxItems: Int = Int.MAX_VALUE, val create: suspend () -> T) {
	var createdItems = AtomicInteger()
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
		return if (createdItems.get() >= maxItems) {
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