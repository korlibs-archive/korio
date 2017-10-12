@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vertx

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.await
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import io.vertx.core.AsyncResult
import io.vertx.core.Handler

fun <T : Any?> Promise.Deferred<T>.toVertxHandler(): Handler<AsyncResult<T>> {
	val deferred = this
	return Handler<AsyncResult<T>> { event ->
		val result = event.result()
		if (result != null) {
			deferred.resolve(event.result())
		} else {
			var cause = event.cause()
			if (cause == null) cause = RuntimeException("Invalid")
			deferred.reject(cause)
		}
	}
}

/*
inline suspend fun <T> vx(noinline callback: (Handler<AsyncResult<T>>) -> Unit) = korioSuspendCoroutine<T> { c ->
	callback(object : Handler<AsyncResult<T>> {
		override fun handle(event: AsyncResult<T>) {
			if (event.succeeded()) {
				c.resume(event.result())
			} else {
				c.resumeWithException(event.cause())
			}
		}
	})
}
*/

inline suspend fun <T> vx(crossinline callback: (Handler<AsyncResult<T>>) -> Unit) = korioSuspendCoroutine<T> { c ->
	callback(Handler<AsyncResult<T>> { event ->
		if (event.succeeded()) {
			c.resume(event.result())
		} else {
			c.resumeWithException(event.cause())
		}
	})
}

suspend fun <T : Any?> vxResult(callback: suspend () -> T): AsyncResult<T> {
	var succeeded = false
	var failed = false
	var result: T? = null
	var cause: Throwable? = null
	try {
		result = callback.await()
		succeeded = true
	} catch (e: Throwable) {
		cause = e
		failed = true
	}
	return object : AsyncResult<T> {
		override fun succeeded(): Boolean = succeeded
		override fun failed(): Boolean = failed
		override fun cause(): Throwable? = cause
		override fun result(): T? = result
	}
}