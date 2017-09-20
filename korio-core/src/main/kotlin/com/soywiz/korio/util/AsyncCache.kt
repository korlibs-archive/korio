package com.soywiz.korio.util

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.async
import com.soywiz.korio.coroutine.withCoroutineContext

class AsyncCache {
	@PublishedApi
	internal val promises = LinkedHashMap<String, Promise<*>>()

	@Suppress("UNCHECKED_CAST")
	suspend operator fun <T> invoke(key: String, gen: suspend () -> T): T = withCoroutineContext {
		return@withCoroutineContext (promises.getOrPut(key) { async(this@withCoroutineContext, gen) } as Promise<T>).await()
	}
}

class AsyncCacheItem<T> {
	@PublishedApi
	internal var promise: Promise<T>? = null

	@Suppress("UNCHECKED_CAST")
	suspend operator fun invoke(gen: suspend () -> T): T = withCoroutineContext {
		if (promise == null) promise = async(this@withCoroutineContext, gen)
		return@withCoroutineContext promise!!.await()
	}
}