package com.soywiz.korio.async

import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.coroutine.korioStartCoroutine
import com.soywiz.korio.coroutine.withCoroutineContext

//class AsyncQueue(val context: CoroutineContext) {
class AsyncQueue() {
	//constructor() : AsyncQueue(CoroutineContext())

	private var promise: Promise<Any> = Promise.resolved(Unit)

	//companion object {
	//	suspend operator fun invoke() = AsyncQueue(getCoroutineContext())
	//}

	operator suspend fun invoke(func: suspend () -> Unit): AsyncQueue = withCoroutineContext { invoke(this@withCoroutineContext, func) }

	operator fun invoke(context: CoroutineContext, func: suspend () -> Unit): AsyncQueue {
		//operator fun invoke(func: suspend () -> Unit): AsyncQueue {
		val oldPromise = this@AsyncQueue.promise
		val newDeferred = Promise.Deferred<Any>()
		this@AsyncQueue.promise = newDeferred.promise
		oldPromise.always {
			func.korioStartCoroutine(newDeferred.toContinuation(context))
		}
		return this@AsyncQueue
	}

	suspend fun await(func: suspend () -> Unit) {
		invoke(func)
		await()
	}

	suspend fun await() = promise.await()
}

fun AsyncQueue.withContext(ctx: CoroutineContext) = AsyncQueueWithContext(this, ctx)
suspend fun AsyncQueue.withContext() = AsyncQueueWithContext(this, getCoroutineContext())

class AsyncQueueWithContext(val queue: AsyncQueue, val context: CoroutineContext) {
	operator fun invoke(func: suspend () -> Unit): AsyncQueue = queue.invoke(context, func)
	suspend fun await(func: suspend () -> Unit) = queue.await(func)
	suspend fun await() = queue.await()
}

class AsyncThread {
	private var lastPromise: Promise<*> = Promise.resolved(Unit)

	fun cancel(): AsyncThread {
		lastPromise.cancel()
		lastPromise = Promise.resolved(Unit)
		return this
	}

	suspend fun <T> cancelAndQueue(func: suspend () -> T): T {
		cancel()
		return queue(func)
	}

	suspend fun <T> queue(func: suspend () -> T): T = invoke(func)

	operator suspend fun <T> invoke(func: suspend () -> T): T {
		val ctx = getCoroutineContext()
		val newDeferred = Promise.Deferred<T>()
		lastPromise.always {
			func.korioStartCoroutine(newDeferred.toContinuation(ctx))
		}
		lastPromise = newDeferred.promise
		return newDeferred.promise.await() as T
	}

	suspend fun <T> sync(func: suspend () -> T): Promise<T> = withCoroutineContext { sync(this@withCoroutineContext, func) }

	fun <T> sync(context: CoroutineContext, func: suspend () -> T): Promise<T> {
		val newDeferred = Promise.Deferred<T>()
		lastPromise.always {
			func.korioStartCoroutine(newDeferred.toContinuation(context))
		}
		lastPromise = newDeferred.promise
		return newDeferred.promise
	}
}

//@Deprecated("AsyncQueue", ReplaceWith("AsyncQueue"))
//typealias WorkQueue = AsyncQueue