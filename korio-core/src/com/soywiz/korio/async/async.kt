@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.coroutine.*
import com.soywiz.korio.util.OS
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.startCoroutine

val workerLazyPool by lazy { Executors.newFixedThreadPool(4) }
val tasksInProgress = AtomicInteger(0)

inline suspend fun <T> suspendCoroutineEL(crossinline block: (Continuation<T>) -> Unit): T = _korioSuspendCoroutine { c ->
	block(c.toEventLoop())
}

fun <T> Continuation<T>.toEventLoop(): Continuation<T> {
	val parent = this
	return object : Continuation<T> {
		override val context: CoroutineContext = parent.context
		override fun resume(value: T) = context.eventLoop.queue { parent.resume(value) }
		override fun resumeWithException(exception: Throwable) = context.eventLoop.queue { parent.resumeWithException(exception) }
	}
}

interface CheckRunning {
	val coroutineContext: CoroutineContext
	val cancelled: Boolean
	fun checkCancelled(): Unit
}

val CheckRunning.eventLoop get() = coroutineContext.eventLoop

suspend fun <T> executeInNewThread(task: suspend () -> T): T = suspendCancellableCoroutine<T> { c ->
	Thread {
		// @TODO: Check this
		task.startCoroutine(c)
	}.apply {
		isDaemon = true
	}.start()
}


fun <T> Promise<T>.jvmSyncAwait(): T {
	var completed = false
	val lock = Any()
	var error: Throwable? = null
	var result: T? = null

	this.then(resolved = {
		synchronized(lock) {
			completed = true
			result = it
		}
	}, rejected = {
		synchronized(lock) {
			completed = true
			error = it
		}
	})

	while (true) {
		synchronized(lock) {
			if (completed) {
				if (error != null) throw error!!
				if (result != null) return result!!
				throw IllegalStateException()
			}
		}
		Thread.sleep(10)
	}
}

suspend fun <T> executeInWorkerSync(task: CheckRunning.() -> T): T = suspendCancellableCoroutine<T> { c ->
	//println("executeInWorker")
	tasksInProgress.incrementAndGet()
	workerLazyPool.execute {
		val checkRunning = object : CheckRunning {
			override var cancelled = false
			override val coroutineContext: CoroutineContext get() = c.context

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
			c.resume(task(checkRunning))
		} catch (t: Throwable) {
			c.resumeWithException(t)
		} finally {
			tasksInProgress.decrementAndGet()
		}
	}
}

suspend fun <T> executeInWorker(task: suspend CheckRunning.() -> T): T = suspendCancellableCoroutine<T> { c ->
	//println("executeInWorker")
	tasksInProgress.incrementAndGet()
	workerLazyPool.execute {
		val checkRunning = object : CheckRunning {
			override var cancelled = false

			override val coroutineContext: CoroutineContext = c.context

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
			task.korioStartCoroutine(checkRunning, c)
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

fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
	sync(el = EventLoopTest(), step = 10, block = block)
}

fun <TEventLoop : EventLoop> sync(el: TEventLoop, step: Int = 10, block: suspend TEventLoop.() -> Unit): Unit {
	if (OS.isJs) throw UnsupportedOperationException("sync block is not supported on javascript target. It is intended for testing.")
	var result: Any? = null

	tasksInProgress.incrementAndGet()
	block.korioStartCoroutine(el, object : Continuation<Unit> {
		override val context: CoroutineContext = el.coroutineContext

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
}

// Wait for a suspension block for testing purposes
fun <T> sync(block: suspend () -> T): T {
	if (OS.isJs) throw UnsupportedOperationException("sync block is not supported on javascript target. It is intended for testing.")
	var result: Any? = null

	val el = eventLoopFactoryDefaultImpl.createEventLoop()
	tasksInProgress.incrementAndGet()
	block.korioStartCoroutine(object : Continuation<T> {
		override val context: CoroutineContext = CoroutineCancelContext() + EventLoopCoroutineContext(el)

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

suspend fun parallel(vararg tasks: suspend () -> Unit) = withCoroutineContext {
	tasks.map { go(this@withCoroutineContext, it) }.await()
}

fun <T> spawn(context: CoroutineContext, task: suspend () -> T): Promise<T> {
	val deferred = Promise.Deferred<T>()
	task.korioStartCoroutine(deferred.toContinuation(context))
	return deferred.promise
}

suspend fun <T> spawn(task: suspend () -> T): Promise<T> = withCoroutineContext {
	val deferred = Promise.Deferred<T>()
	task.korioStartCoroutine(deferred.toContinuation(this@withCoroutineContext))
	return@withCoroutineContext deferred.promise
}

// Aliases for spawn
fun <T> async(context: CoroutineContext, task: suspend () -> T): Promise<T> = spawn(context, task)

suspend fun <T> async(task: suspend () -> T): Promise<T> = withCoroutineContext { spawn(this@withCoroutineContext, task) }

fun <T> go(context: CoroutineContext, task: suspend () -> T): Promise<T> = spawn(context, task)

suspend fun <T> go(task: suspend () -> T): Promise<T> = withCoroutineContext { spawn(this@withCoroutineContext, task) }

suspend fun <R, T> (suspend R.() -> T).await(receiver: R): T = korioSuspendCoroutine { c ->
	this.korioStartCoroutine(receiver, c)
}

suspend fun <T> (suspend () -> T).await(): T = korioSuspendCoroutine { c ->
	this.korioStartCoroutine(c)
}

fun <R, T> (suspend R.() -> T).execAndForget(context: CoroutineContext, receiver: R) = spawnAndForget(context) {
	this.await(receiver)
}

fun <T> (suspend () -> T).execAndForget(context: CoroutineContext) = spawnAndForget(context) {
	this.await()
}


class EmptyContinuation(override val context: CoroutineContext) : Continuation<Any> {
	override fun resume(value: Any) = Unit
	override fun resumeWithException(exception: Throwable) = exception.printStackTrace()
}


@Suppress("UNCHECKED_CAST")
inline fun <T> spawnAndForget(context: CoroutineContext, noinline task: suspend () -> T): Unit = task.korioStartCoroutine(EmptyContinuation(context) as Continuation<T>)

suspend fun <T> spawnAndForget(task: suspend () -> T): Unit = withCoroutineContext { spawnAndForget(task) }

inline fun <T> spawnAndForget(context: CoroutineContext, value: T, noinline task: suspend T.() -> Any): Unit = task.korioStartCoroutine(value, EmptyContinuation(context))
