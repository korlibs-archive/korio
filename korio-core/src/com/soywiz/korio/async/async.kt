@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.util.OS
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.*

val workerLazyPool by lazy { Executors.newCachedThreadPool() }
var tasksInProgress = AtomicInteger(0)

inline suspend fun <T> suspendCoroutineEL(crossinline block: (Continuation<T>) -> Unit): T = suspendCoroutine { c ->
	block(c.toEventLoop())
}

fun <T> Continuation<T>.toEventLoop(): Continuation<T> {
	val parent = this
	return object : Continuation<T> {
		override val context: CoroutineContext = parent.context
		override fun resume(value: T) = EventLoop.queue { parent.resume(value) }
		override fun resumeWithException(exception: Throwable) = EventLoop.queue { parent.resumeWithException(exception) }
	}
}

interface CheckRunning {
	val cancelled: Boolean
	fun checkCancelled(): Unit
}

suspend fun <T> executeInWorker(task: suspend CheckRunning.() -> T): T = suspendCancellableCoroutine<T> { c ->
	tasksInProgress.incrementAndGet()
	workerLazyPool.execute {
		val checkRunning = object : CheckRunning {
			override var cancelled = false

			init {
				c.onCancel {
					cancelled = true
				}
			}

			override fun checkCancelled() {
				if (cancelled) throw CancellationException()
			}
		}

		try {
			task.startCoroutine(checkRunning, c)
		} catch (t: Throwable) {
			c.resumeWithException(t)
		} finally {
			tasksInProgress.decrementAndGet()
		}
	}
}

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

fun <TEventLoop : EventLoop> sync(el: TEventLoop, step: Int = 10, block: suspend TEventLoop.() -> Unit): Unit {
	val oldEl = EventLoop._impl
	EventLoop._impl = el
	try {
		if (OS.isJs) throw UnsupportedOperationException("sync block is not supported on javascript target. It is intended for testing.")
		var result: Any? = null

		tasksInProgress.incrementAndGet()
		block.startCoroutine(el, object : Continuation<Unit> {
			override val context: CoroutineContext = EmptyCoroutineContext

			override fun resume(value: Unit) = run {
				tasksInProgress.decrementAndGet()
				result = value
			}

			override fun resumeWithException(exception: Throwable) = run {
				tasksInProgress.decrementAndGet()
				result = exception
			}
		})

		while (result == null) {
			Thread.sleep(1L)
			el.step(step)
		}
		if (result is Throwable) throw result as Throwable
		return Unit
	} finally {
		EventLoop._impl = oldEl
	}
}

// Wait for a suspension block for testing purposes
fun <T> sync(block: suspend () -> T): T {
	if (OS.isJs) throw UnsupportedOperationException("sync block is not supported on javascript target. It is intended for testing.")
	var result: Any? = null

	tasksInProgress.incrementAndGet()
	block.startCoroutine(object : Continuation<T> {
		override val context: CoroutineContext = CoroutineCancelContext()

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
	@Suppress("UNCHECKED_CAST")
	return result as T
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

fun <R, T> (suspend R.() -> T).execAndForget(receiver: R) = spawnAndForget {
	this.await(receiver)
}

fun <T> (suspend () -> T).execAndForget() = spawnAndForget {
	this.await()
}

object EmptyContinuation : Continuation<Any> {
	override val context: CoroutineContext = EmptyCoroutineContext
	override fun resume(value: Any) = Unit
	override fun resumeWithException(exception: Throwable) = exception.printStackTrace()
}

@Suppress("UNCHECKED_CAST")
inline fun <T> spawnAndForget(task: suspend () -> T): Unit = task.startCoroutine(EmptyContinuation as Continuation<T>)

inline fun <T> spawnAndForget(value: T, task: suspend T.() -> Any): Unit = task.startCoroutine(value, EmptyContinuation)
