package com.soywiz.korio.async

import com.soywiz.korio.util.OS
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.*

val workerLazyPool by lazy { Executors.newCachedThreadPool() }
var tasksInProgress = AtomicInteger(0)

inline suspend fun <T> asyncFun(routine: suspend () -> T): T = suspendCoroutine<T> {
	routine.startCoroutine(it)
}

suspend fun <T> executeInWorker(task: suspend () -> T): T = suspendCoroutine<T> { c ->
	tasksInProgress.incrementAndGet()
	workerLazyPool.execute {
		try {
			task.startCoroutine(c)
		} catch (t: Throwable) {
			c.resumeWithException(t)
		} finally {
			tasksInProgress.decrementAndGet()
		}
	}
}

// Wait for a suspension block for testing purposes
fun <T> sync(block: suspend () -> T): T {
	if (OS.isJs) {
		throw UnsupportedOperationException("sync block is not supported on javascript target")
	} else {
		var result: Any? = null

		tasksInProgress.incrementAndGet()
		block.startCoroutine(object : Continuation<T> {
			override fun resume(value: T) = run {
				tasksInProgress.decrementAndGet()
				result = value
			}

			override fun resumeWithException(exception: Throwable) = run {
				tasksInProgress.decrementAndGet()
				result = exception
			}
		})

		while (result == null) Thread.sleep(1L)
		if (result is Throwable) throw result as Throwable
		return result as T
	}
}

fun <T> spawn(task: suspend () -> T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	task.startCoroutine(deferred.toContinuation())
	return deferred.promise
}

// Aliases for spawn
fun <T> async(task: suspend () -> T): Promise<T> = spawn(task)

fun <T> go(task: suspend () -> T): Promise<T> = spawn(task)

suspend fun <R, T> (suspend R.() -> T).await(receiver: R): T = suspendCoroutine { c ->
	this.startCoroutine(receiver, c)
}

suspend fun <T> (suspend () -> T).await(): T = suspendCoroutine { c ->
	this.startCoroutine(c)
}

object EmptyContinuation : Continuation<Any> {
	override fun resume(value: Any) = Unit
	override fun resumeWithException(exception: Throwable) {
		exception.printStackTrace()
	}
}

inline fun spawnAndForget(task: suspend () -> Any): Unit = task.startCoroutine(EmptyContinuation)
inline fun <T> spawnAndForget(value: T, task: suspend T.() -> Any): Unit = task.startCoroutine(value, EmptyContinuation)
