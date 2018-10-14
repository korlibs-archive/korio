package com.soywiz.korio.async

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

fun <T> CompletableDeferred<T>.toContinuation(context: CoroutineContext, job: Job? = null): Continuation<T> {
	val deferred = CompletableDeferred<T>(job)
	return object : Continuation<T> {
		override val context: CoroutineContext = context

		override fun resumeWith(result: Result<T>) {
			val exception = result.exceptionOrNull()
			if (exception != null) {
				deferred.cancel(exception)
			} else {
				deferred.complete(result.getOrThrow())
			}
		}
	}
}
