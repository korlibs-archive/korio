package com.soywiz.korio.util

import com.soywiz.korio.async.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

class AsyncCache {
	@PublishedApi
	internal val promises = LinkedHashMap<String, Deferred<*>>()

	@Suppress("UNCHECKED_CAST")
	suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T {
		return (promises.getOrPut(key) { asyncImmediately(coroutineContext) { gen() } } as Deferred<T>).await()
	}
}

class AsyncCacheItem<T> {
	@PublishedApi
	internal var promise: Deferred<T>? = null

	@Suppress("UNCHECKED_CAST")
	suspend operator fun invoke(gen: suspend () -> T): T {
		if (promise == null) promise = asyncImmediately(coroutineContext) { gen() }
		return promise!!.await()
	}
}
