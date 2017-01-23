package com.soywiz.korio.async

import com.soywiz.korio.util.Cancellable
import java.util.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

//inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (CancellableContinuation<T>) -> Unit): T = suspendCoroutineOrReturn { c ->
inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (CancellableContinuation<T>) -> Unit): T = suspendCoroutineEL { c ->
	//inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (Continuation<T>) -> Unit): T = suspendCoroutineEL { c ->
	//block(c)
	block(CancellableContinuation(c))
}

class CoroutineCancelContext() : AbstractCoroutineContextElement(CoroutineCancelContext.Key) {
	private val handlers = LinkedList<(Throwable) -> Unit>()

	fun exec(c: Throwable) {
		while (true) {
			val f = synchronized(handlers) { if (handlers.isNotEmpty()) handlers.removeFirst() else null } ?: break
			f.invoke(c)
		}
	}

	fun add(handler: (Throwable) -> Unit) {
		synchronized(handlers) { handlers += handler }
	}

	companion object Key : CoroutineContext.Key<CoroutineCancelContext>

	override fun toString(): String = "CoroutineCancelSignal(${handlers.size})"
}

class CancellableContinuation<in T>(private val delegate: Continuation<T>) : Continuation<T>, Cancellable, Cancellable.Listener {
	override val context: CoroutineContext = if (delegate.context[CoroutineCancelContext.Key] != null) delegate.context else CoroutineCancelContext() + delegate.context
	val cancelContext = context[CoroutineCancelContext.Key]!!

	var completed = false
	var cancelled = false

	override fun resume(value: T) {
		if (completed || cancelled) return
		completed = true
		delegate.resume(value)
	}

	override fun onCancel(handler: (Throwable) -> Unit) {
		cancelContext.add(handler)
	}

	override fun cancel(e: Throwable) {
		if (completed || cancelled) return
		cancelled = true
		cancelContext.exec(e)
		delegate.resumeWithException(e)
	}

	override fun resumeWithException(exception: Throwable) {
		if (completed || cancelled) return
		completed = true
		delegate.resumeWithException(exception)
	}
}
