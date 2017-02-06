package com.soywiz.korio.util

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async

class AsyncCache {
	@PublishedApi
	internal val promises = LinkedHashMap<String, Promise<*>>()

	@Suppress("UNCHECKED_CAST")
	suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T {
		return (promises.getOrPut(key) { async(gen) } as Promise<T>).await()
	}
}

class AsyncCacheItem<T> {
	@PublishedApi
	internal var promise: Promise<T>? = null

	@Suppress("UNCHECKED_CAST")
	suspend operator fun invoke(gen: suspend () -> T): T {
		if (promise == null) promise = async(gen)
		return promise!!.await()
	}
}