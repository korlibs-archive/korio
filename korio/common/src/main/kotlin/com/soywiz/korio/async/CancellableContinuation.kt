package com.soywiz.korio.async

import com.soywiz.korio.coroutine.AbstractCoroutineContextElement
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.CoroutineContextKey
import com.soywiz.kds.LinkedList
import com.soywiz.korio.util.Cancellable

//inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (CancellableContinuation<T>) -> Unit): T = suspendCoroutineOrReturn { c ->
inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (CancellableContinuation<T>) -> Unit): T = suspendCoroutineEL { c ->
	//inline suspend fun <T> suspendCancellableCoroutine(crossinline block: (Continuation<T>) -> Unit): T = suspendCoroutineEL { c ->
	//block(c)
	block(CancellableContinuation(c))
}

class CoroutineCancelContext : AbstractCoroutineContextElement(CoroutineCancelContext.Key) {
	companion object Key : CoroutineContextKey<CoroutineCancelContext>

	private val handlers = LinkedList<(Throwable) -> Unit>()
	private var c: Throwable? = null

	fun exec(c: Throwable) {
		this.c = c
		flush()
	}

	fun add(handler: (Throwable) -> Unit): (Throwable) -> Unit {
		synchronized(handlers) { handlers += handler }
		flush()
		return handler
	}

	fun remove(handler: (Throwable) -> Unit): (Throwable) -> Unit {
		synchronized(handlers) { handlers -= handler }
		return handler
	}

	private fun flush() {
		val c = this.c
		if (c != null) {
			while (true) {
				val f = synchronized(handlers) { if (handlers.isNotEmpty()) handlers.removeFirst() else null } ?: break
				f.invoke(c)
			}
		}
	}

	override fun toString(): String = "CoroutineCancelSignal(${handlers.size})"
}

class CancellableContinuation<in T>(private val delegate: Continuation<T>) : Continuation<T>, Cancellable, Cancellable.Listener {
	override val context: CoroutineContext = if (delegate.context[CoroutineCancelContext.Key] != null) delegate.context else CoroutineCancelContext() + delegate.context
	val cancelContext = context[CoroutineCancelContext.Key]!!

	private val cancells = arrayListOf<(Throwable) -> Unit>()

	var completed = false

	private var _cancelled: Boolean = false
	private var _cancelledHandler: Boolean = false
	val cancelled: Boolean
		get() {
			if (!_cancelledHandler) {
				cancelContext.add { _cancelled = true }
				_cancelledHandler = true
			}
			return _cancelled
		}

	override fun resume(value: T) {
		if (completed || _cancelled) return
		completed = true
		cancelHandlers()
		delegate.resume(value)
	}

	private fun cancelHandlers() {
		for (c in cancells) cancelContext.remove(c)
		cancells.clear()
	}

	override fun onCancel(handler: (Throwable) -> Unit) {
		cancells += cancelContext.add(handler)
	}

	override fun cancel(e: Throwable) {
		if (completed || _cancelled) return
		_cancelled = true
		cancelContext.exec(e)
		delegate.resumeWithException(e)
		cancelHandlers()
	}

	override fun resumeWithException(exception: Throwable) {
		if (completed || _cancelled) return
		completed = true
		cancelHandlers()
		delegate.resumeWithException(exception)
	}
}
