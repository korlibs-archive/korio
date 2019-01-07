package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

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

@UseExperimental(InternalCoroutinesApi::class)
class TestCoroutineDispatcher(val frameTime: TimeSpan = 16.milliseconds) :
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
		scheduleAfter(frameTime.millisecondsInt) { continuation.resume(Unit) }
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

// @TODO: Kotlin.JS bug!
//fun suspendTestExceptJs(callback: suspend () -> Unit) = suspendTest {
//	if (OS.isJs) return@suspendTest
//	callback()
//}

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

expect fun asyncEntryPoint(callback: suspend () -> Unit)
fun suspendTest(callback: suspend () -> Unit) = asyncEntryPoint(callback)
fun suspendTest(context: CoroutineContext, callback: suspend () -> Unit) = suspendTest { withContext(context) { callback() } }
fun suspendTestExceptJs(callback: suspend () -> Unit) {
	if (!OS.isJs) {
		suspendTest {
			callback()
		}
	}
}

