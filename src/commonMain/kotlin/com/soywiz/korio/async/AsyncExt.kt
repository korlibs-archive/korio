package com.soywiz.korio.async

import com.soywiz.kds.PriorityQueue
import com.soywiz.klock.TimeSpan
import com.soywiz.korio.KorioNative
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.util.OS
import kotlinx.coroutines.*
import kotlin.coroutines.*

// @TODO: BUG: kotlin-js bug :: Uncaught ReferenceError: CoroutineImpl is not defined
//Coroutine$await$lambda.$metadata$ = {kind: Kotlin.Kind.CLASS, simpleName: null, interfaces: [CoroutineImpl]};
//Coroutine$await$lambda.prototype = Object.create(CoroutineImpl.prototype);

//suspend inline fun <T, R> (suspend T.() -> R).await(receiver: T): R = withContext(coroutineContext.dispatcher) { this(receiver) }
//suspend inline fun <R> (suspend () -> R).await(): R = withContext(coroutineContext.dispatcher) { this() }

suspend fun <T, R> (suspend T.() -> R).await(receiver: T): R =
	withContext(coroutineContext.dispatcher) { this@await(receiver) }

suspend fun <R> (suspend () -> R).await(): R = withContext(coroutineContext.dispatcher) { this@await() }

// @TODO: Try to get in subinstance
val CoroutineContext.tryDispatcher: CoroutineDispatcher? get() = this as? CoroutineDispatcher?
val CoroutineContext.dispatcher: CoroutineDispatcher get() = this.tryDispatcher ?: KorioDefaultDispatcher

// @TODO: Do this better! (JS should use requestAnimationFrame)
suspend fun delayNextFrame() = _delayNextFrame()

interface DelayFrame {
	fun delayFrame(continuation: CancellableContinuation<Unit>) {
		launchImmediately(continuation.context) {
			delay(16)
			continuation.resume(Unit)
		}
	}
}

suspend fun DelayFrame.delayFrame() = suspendCancellableCoroutine<Unit> { c -> delayFrame(c) }

val DefaultDelayFrame: DelayFrame = object : DelayFrame {}

val CoroutineContext.delayFrame: DelayFrame
	get() = get(ContinuationInterceptor) as? DelayFrame ?: DefaultDelayFrame


private suspend fun _delayNextFrame() {
	coroutineContext.delayFrame.delayFrame()
}

suspend fun CoroutineContext.delayNextFrame() {
	delayFrame.delayFrame()
}

suspend fun CoroutineContext.delayMs(time: Long) {
	withContext(this) {
		kotlinx.coroutines.delay(time)
	}
}

suspend fun delay(time: TimeSpan): Unit = delay(time.millisecondsLong)

suspend fun CoroutineContext.delay(time: TimeSpan) = delayMs(time.millisecondsLong)

fun CoroutineScope.animationFrameLoop(callback: suspend (Closeable) -> Unit): Closeable {
	var running = true
	val close = Closeable {
		running = false
	}
	launchImmediately {
		while (running) {
			callback(close)
			delayNextFrame()
		}
	}
	return close
}

@InternalCoroutinesApi
class TestCoroutineDispatcher(val frameTime: Int = 16) :
	AbstractCoroutineContextElement(ContinuationInterceptor),
	ContinuationInterceptor,
	Delay, DelayFrame {
	var time = 0L; private set

	class TimedTask(val time: Long, val callback: suspend () -> Unit) {
		override fun toString(): String = "TimedTask(time=$time)"
	}

	val tasks = PriorityQueue<TimedTask>(Comparator { a, b -> a.time.compareTo(b.time) })

	override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
		return object : Continuation<T> {
			override val context: CoroutineContext = continuation.context

			override fun resumeWith(result: Result<T>) {
				val exception = result.exceptionOrNull()
				if (exception != null) {
					continuation.resumeWithException(exception)
				} else {
					continuation.resume(result.getOrThrow())
				}
			}
		}
	}

	private fun scheduleAfter(time: Int, callback: suspend () -> Unit) {
		tasks += TimedTask(this.time + time) {
			callback()
		}
	}

	fun dispatch(context: CoroutineContext, block: Runnable) {
		scheduleAfter(0) { block.run() }
	}

	override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>): Unit {
		scheduleAfter(timeMillis.toInt()) { continuation.resume(Unit) }
	}

	override fun delayFrame(continuation: CancellableContinuation<Unit>) {
		scheduleAfter(frameTime) { continuation.resume(Unit) }
	}

	var exception: Throwable? = null
	fun loop() {
		//println("doStep: currentThreadId=$currentThreadId")
		if (exception != null) throw exception ?: error("error")
		//println("TASKS: ${tasks.size}")
		while (tasks.isNotEmpty()) {
			val task = tasks.removeHead()!!
			this.time = task.time
			//println("RUN: $task")
			task.callback.startCoroutine(object : Continuation<Unit> {
				override val context: CoroutineContext = this@TestCoroutineDispatcher

				override fun resumeWith(result: Result<Unit>) {
					val exception = result.exceptionOrNull()
					exception?.printStackTrace()
					this@TestCoroutineDispatcher.exception = exception
				}
			})
		}
	}

	fun loop(entry: suspend () -> Unit) {
		entry.startCoroutine(object : Continuation<Unit> {
			override val context: CoroutineContext = this@TestCoroutineDispatcher

			override fun resumeWith(result: Result<Unit>) {
				val exception = result.exceptionOrNull()
				exception?.printStackTrace()
				this@TestCoroutineDispatcher.exception = exception
			}
		})
		loop()
	}
}

fun suspendTest(callback: suspend () -> Unit) = KorioNative.suspendTest { callback() }

fun suspendTest(context: CoroutineContext, callback: suspend () -> Unit) =
	KorioNative.suspendTest { withContext(context) { callback() } }

// @TODO: Kotlin.JS bug!
//fun suspendTestExceptJs(callback: suspend () -> Unit) = suspendTest {
//	if (OS.isJs) return@suspendTest
//	callback()
//}

fun suspendTestExceptJs(callback: suspend () -> Unit) {
	if (!OS.isJs) {
		suspendTest {
			callback()
		}
	}
}

fun CoroutineScope.launchImmediately(callback: suspend () -> Unit) =
	launch(coroutineContext, start = CoroutineStart.UNDISPATCHED) {
		try {
			callback()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTrace()
			throw e
		}
	}

fun CoroutineScope.launchAsap(callback: suspend () -> Unit) =
	launch(coroutineContext, start = CoroutineStart.DEFAULT) {
		try {
			callback()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTrace()
			throw e
		}
	}

fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T) =
	async(coroutineContext, start = CoroutineStart.UNDISPATCHED) {
		try {
			callback()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTrace()
			throw e
		}
	}

fun <T> CoroutineScope.asyncAsap(callback: suspend () -> T) =
	async(coroutineContext, start = CoroutineStart.DEFAULT) {
		try {
			callback()
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			e.printStackTrace()
			throw e
		}
	}


fun launchImmediately(context: CoroutineContext, callback: suspend () -> Unit) =
	CoroutineScope(context).launchImmediately(callback)

fun launchAsap(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchAsap(callback)
fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) =
	CoroutineScope(context).asyncImmediately(callback)

fun <T> asyncAsap(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncAsap(callback)
