package com.soywiz.korio.async

import kotlinx.coroutines.*
import kotlin.coroutines.*

//class AsyncQueue(val context: CoroutineContext) {
class AsyncQueue {
	//constructor() : AsyncQueue(CoroutineContext())

	val thread = AsyncThread()

	//companion object {
	//	suspend operator fun invoke() = AsyncQueue(getCoroutineContext())
	//}

	suspend operator fun invoke(func: suspend () -> Unit): AsyncQueue = invoke(coroutineContext, func)

	operator fun invoke(context: CoroutineContext, func: suspend () -> Unit): AsyncQueue {
		thread.sync(context) { func() }
		return this
	}

	suspend fun await(func: suspend () -> Unit) {
		invoke(func)
		await()
	}

	suspend fun await() {
		thread.await()
	}
}

fun AsyncQueue.withContext(ctx: CoroutineContext) = AsyncQueueWithContext(this, ctx)
suspend fun AsyncQueue.withContext() = AsyncQueueWithContext(this, coroutineContext)

class AsyncQueueWithContext(val queue: AsyncQueue, val context: CoroutineContext) {
	operator fun invoke(func: suspend () -> Unit): AsyncQueue = queue.invoke(context, func)
	suspend fun await(func: suspend () -> Unit) = queue.await(func)
	suspend fun await() = queue.await()
}

class AsyncThread {
	private var lastPromise: Deferred<*> = CompletableDeferred(Unit).apply {
		this.complete(Unit)
	}

	suspend fun await() {
		while (true) {
			val cpromise = lastPromise
			lastPromise.await()
			if (cpromise == lastPromise) break
		}
	}

	fun cancel(): AsyncThread {
		lastPromise.cancel()
		lastPromise = CompletableDeferred(Unit)
		return this
	}

	suspend fun <T> cancelAndQueue(func: suspend () -> T): T {
		cancel()
		return queue(func)
	}

	suspend fun <T> queue(func: suspend () -> T): T = invoke(func)

	suspend operator fun <T> invoke(func: suspend () -> T): T {
		val task = sync(coroutineContext, func)
		try {
			val res = task.await()
			return res
		} catch (e: Throwable) {
			throw e
		}
	}

	suspend fun <T> sync(func: suspend () -> T): Deferred<T> = sync(coroutineContext, func)

	fun <T> sync(context: CoroutineContext, func: suspend () -> T): Deferred<T> {
		val oldPromise = lastPromise
		val promise = asyncImmediately(context) {
			oldPromise.await()
			func()
		}
		lastPromise = promise
		return promise

	}
}

//@Deprecated("AsyncQueue", ReplaceWith("AsyncQueue"))
//typealias WorkQueue = AsyncQueue