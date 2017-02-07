package com.soywiz.korio.async

import com.soywiz.korio.coroutine.korioStartCoroutine

class AsyncQueue {
	private var promise: Promise<Any> = Promise.resolved(Unit)

	operator fun invoke(func: suspend () -> Unit): AsyncQueue {
		val oldPromise = this.promise
		val newDeferred = Promise.Deferred<Any>()
		this.promise = newDeferred.promise
		oldPromise.always {
			func.korioStartCoroutine(newDeferred.toContinuation())
		}
		return this
	}

	suspend fun await(func: suspend () -> Unit): Unit {
		invoke(func)
		await()
	}

	suspend fun await(): Unit {
		promise.await()
	}
}

class AsyncThread {
	private var lastPromise: Promise<*> = Promise.resolved(Unit)

	operator suspend fun <T> invoke(func: suspend () -> T): T {
		val newDeferred = Promise.Deferred<T>()
		lastPromise.always {
			func.korioStartCoroutine(newDeferred.toContinuation())
		}
		lastPromise = newDeferred.promise
		return newDeferred.promise.await() as T
	}
}

@Deprecated("AsyncQueue", ReplaceWith("AsyncQueue"))
typealias WorkQueue = AsyncQueue