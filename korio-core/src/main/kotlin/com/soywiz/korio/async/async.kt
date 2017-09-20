@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.coroutine.*
import com.soywiz.korio.lang.printStackTrace

header suspend fun <T> executeInWorker(callback: suspend () -> T): T

inline suspend fun <T> suspendCoroutineEL(crossinline block: (Continuation<T>) -> Unit): T = _korioSuspendCoroutine { c ->
	block(c.toEventLoop())
}

fun <T> Continuation<T>.toEventLoop(): Continuation<T> {
	val parent = this
	return object : Continuation<T> {
		override val context: CoroutineContext = parent.context
		override fun resume(value: T): Unit = run { context.eventLoop.queue { parent.resume(value) } }
		override fun resumeWithException(exception: Throwable): Unit = run { context.eventLoop.queue { parent.resumeWithException(exception) } }
	}
}

interface CheckRunning {
	val coroutineContext: CoroutineContext
	val cancelled: Boolean
	fun checkCancelled(): Unit
}

val CheckRunning.eventLoop get() = coroutineContext.eventLoop

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

interface CoroutineContextHolder {
	val coroutineContext: CoroutineContext
}


// Aliases for spawn
fun <T> async(context: CoroutineContext, task: suspend () -> T): Promise<T> = spawn(context, task)

fun <T> go(context: CoroutineContext, task: suspend () -> T): Promise<T> = spawn(context, task)

fun <T> CoroutineContextHolder.go(task: suspend () -> T): Promise<T> = spawn(this.coroutineContext, task)
fun <T> CoroutineContextHolder.async(task: suspend () -> T): Promise<T> = spawn(this.coroutineContext, task)
fun <T> CoroutineContextHolder.spawn(task: suspend () -> T): Promise<T> = spawn(this.coroutineContext, task)

suspend fun <T> async(task: suspend CoroutineContext.() -> T): Promise<T> = withCoroutineContext { spawn(this@withCoroutineContext) { task(this@withCoroutineContext) } }
suspend fun <T> go(task: suspend CoroutineContext.() -> T): Promise<T> = withCoroutineContext { spawn(this@withCoroutineContext) { task(this@withCoroutineContext) } }

fun <T> EventLoop.async(task: suspend CoroutineContext.() -> T): Promise<T> = spawn(this@async.coroutineContext) { task(this@async.coroutineContext) }
fun <T> CoroutineContext.async(task: suspend CoroutineContext.() -> T): Promise<T> = spawn(this@async) { task(this@async) }

fun <T> EventLoop.go(task: suspend CoroutineContext.() -> T): Promise<T> = spawn(this@go.coroutineContext) { task(this@go.coroutineContext) }
fun <T> CoroutineContext.go(task: suspend CoroutineContext.() -> T): Promise<T> = spawn(this@go) { task(this@go) }

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

suspend fun <T> spawnAndForget(task: suspend () -> T): Unit = withCoroutineContext { spawnAndForget(this@withCoroutineContext, task) }

inline fun <T> spawnAndForget(context: CoroutineContext, value: T, noinline task: suspend T.() -> Any): Unit = task.korioStartCoroutine(value, EmptyContinuation(context))

fun syncTest(callback: suspend EventLoopTest.() -> Unit): Unit = TODO()