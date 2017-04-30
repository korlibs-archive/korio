package com.soywiz.korio.coroutine

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.eventLoop
import com.soywiz.korio.async.toEventLoop
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.createCoroutine
import kotlin.coroutines.experimental.startCoroutine

//val COROUTINE_SUSPENDED = kotlin.coroutines.experimental.COROUTINE_SUSPENDED
val COROUTINE_SUSPENDED = kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

typealias RestrictsSuspension = kotlin.coroutines.experimental.RestrictsSuspension
typealias Continuation<T> = kotlin.coroutines.experimental.Continuation<T>
typealias CoroutineContext = kotlin.coroutines.experimental.CoroutineContext
typealias CoroutineContextKey<T> = kotlin.coroutines.experimental.CoroutineContext.Key<T>
//typealias EmptyCoroutineContext = kotlin.coroutines.experimental.EmptyCoroutineContext
typealias AbstractCoroutineContextElement = kotlin.coroutines.experimental.AbstractCoroutineContextElement

//inline suspend fun <T> korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T = kotlin.coroutines.experimental.suspendCoroutine(block)


suspend fun <T> withCoroutineContext(callback: suspend CoroutineContext.() -> T) = korioSuspendCoroutine<T> { c ->
	callback.startCoroutine(c.context, c)
}

suspend fun <T> withEventLoop(callback: suspend EventLoop.() -> T) = korioSuspendCoroutine<T> { c ->
	callback.startCoroutine(c.context.eventLoop, c)
}

inline suspend fun <T> korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T = _korioSuspendCoroutine { c ->
	block(c.toEventLoop())
}

inline suspend fun <T> _korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T {
	return kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn { c: Continuation<T> ->
		val unsafe = UnsafeContinuation(c)
		block(unsafe)
		unsafe.getResult()
	}
}

fun <R, T> (suspend R.() -> T).korioStartCoroutine(receiver: R, completion: Continuation<T>) = this.startCoroutine(receiver, completion)
fun <T> (suspend () -> T).korioStartCoroutine(completion: Continuation<T>) = this.startCoroutine(completion)
fun <T> (suspend () -> T).korioCreateCoroutine(completion: Continuation<T>): Continuation<Unit> = this.createCoroutine(completion)
fun <R, T> (suspend R.() -> T).korioCreateCoroutine(receiver: R, completion: Continuation<T>): Continuation<Unit> = this.createCoroutine(receiver, completion)

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()

private class Fail(val exception: Throwable)

@PublishedApi
internal class UnsafeContinuation<in T> @PublishedApi internal constructor(private val delegate: Continuation<T>) : Continuation<T> {
	override val context: CoroutineContext get() = delegate.context

	@Volatile
	private var result: Any? = UNDECIDED

	override fun resume(value: T) {
		val result = this.result
		when {
			result === UNDECIDED -> {
				this.result = value
			}
			result === COROUTINE_SUSPENDED -> {
				this.result = RESUMED
				delegate.resume(value)
			}
			else -> throw java.lang.IllegalStateException("Already resumed")
		}
	}

	override fun resumeWithException(exception: Throwable) {
		val result = this.result
		when {
			result === UNDECIDED -> {
				this.result = Fail(exception)
			}
			result === kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED -> {
				this.result = RESUMED
				delegate.resumeWithException(exception)
				return
			}
			else -> throw java.lang.IllegalStateException("Already resumed")
		}
	}

	@PublishedApi
	internal fun getResult(): Any? {
		val result = this.result
		if (result === UNDECIDED) {
			this.result = kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
			return kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
		}
		when {
			result === RESUMED -> return kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED // already called continuation, indicate SUSPENDED_MARKER upstream
			result is Fail -> throw result.exception
			else -> return result // either SUSPENDED_MARKER or data
		}
	}
}