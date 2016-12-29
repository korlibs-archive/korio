package com.soywiz.korio.async

import com.soywiz.korio.util.OS
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

inline suspend fun <T> asyncFun(routine: suspend () -> T): T = suspendCoroutine<T> { routine.startCoroutine(it) }

val workerLazyPool by lazy { Executors.newCachedThreadPool() }

suspend fun <T> executeInWorker(task: suspend () -> T): T = suspendCoroutine<T> { c ->
	workerLazyPool.execute {
		task.startCoroutine(c)
	}
}

// Wait for a suspension block for testing purposes
fun <T> sync(block: suspend () -> T): T {
	if (OS.isJs) {
		throw UnsupportedOperationException("sync block is not supported on javascript target")
	} else {
		var result: Any? = null

		block.startCoroutine(object : Continuation<T> {
			override fun resume(value: T) = run { result = value }
			override fun resumeWithException(exception: Throwable) = run { result = exception }
		})

		while (result == null) Thread.sleep(1L)
		if (result is Throwable) throw result as Throwable
		return result as T
	}
}

suspend fun <T> spawn(task: suspend () -> T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	task.startCoroutine(deferred.toContinuation())
	return deferred.promise
}

// Aliases for spawn
suspend fun <T> async(task: suspend () -> T): Promise<T> = spawn(task)
suspend fun <T> go(task: suspend () -> T): Promise<T> = spawn(task)
