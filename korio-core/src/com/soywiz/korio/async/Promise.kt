package com.soywiz.korio.async

import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.util.Cancellable
import java.util.*
import java.util.concurrent.CancellationException

class Promise<T : Any?> : Cancellable {
	class Deferred<T : Any?> {
		val promise = Promise<T>()
		val onCancel = promise.onCancel
		fun resolve(value: T): Unit = run { promise.complete(value, null) }
		fun reject(error: Throwable): Unit = run { promise.complete(null, error) }
		fun toContinuation(ctx: CoroutineContext = CoroutineCancelContext()): CancellableContinuation<T> {
			val deferred = this
			val cc = CancellableContinuation(object : Continuation<T> {
				override val context: CoroutineContext = ctx
				override fun resume(value: T) = deferred.resolve(value)
				override fun resumeWithException(exception: Throwable) = deferred.reject(exception)
			})
			onCancel {
				cc.cancel()
			}
			cc.onCancel {
				promise.cancel()
				cc.cancel()
			}
			return cc
		}
	}

	companion object {
		fun <T> resolved(value: T) = Promise<T>().complete(value, null)
		fun <T> rejected(error: Throwable) = Promise<T>().complete(null, error)

		suspend fun <T> create(callback: suspend (deferred: Deferred<T>) -> Unit): T {
			val deferred = Deferred<T>()
			callback(deferred)
			return deferred.promise.await()
		}
	}

	private var value: T? = null
	private var error: Throwable? = null
	private var done: Boolean = false
	private val resolvedHandlers = LinkedList<(T) -> Unit>()
	private val rejectedHandlers = LinkedList<(Throwable) -> Unit>()

	private fun flush() {
		if (!done) return
		if (error != null) {
			while (true) {
				val handler = synchronized(rejectedHandlers) { if (rejectedHandlers.isNotEmpty()) rejectedHandlers.removeFirst() else null } ?: break
				EventLoop.queue { handler(error ?: RuntimeException()) }
			}
		} else {
			while (true) {
				val handler = synchronized(resolvedHandlers) { if (resolvedHandlers.isNotEmpty()) resolvedHandlers.removeFirst() else null } ?: break
				EventLoop.queue { handler(value as T) }
			}
		}
	}

	internal fun complete(value: T?, error: Throwable?): Promise<T> {
		if (!this.done) {
			this.value = value
			this.error = error
			this.done = true

			if (error != null && synchronized(resolvedHandlers) { this.rejectedHandlers.isEmpty() } && error !is CancellationException) {
				System.err.println("## Not handled Promise exception:")
				error.printStackTrace()
			}

			flush()
		}
		return this
	}

	fun then(resolved: (T) -> Unit) {
		synchronized(resolvedHandlers) { resolvedHandlers += resolved }
		flush()
	}

	fun always(resolved: () -> Unit) {
		then(
			resolved = { resolved() },
			rejected = { resolved() }
		)
	}

	fun then(resolved: (T) -> Unit, rejected: (Throwable) -> Unit) {
		synchronized(resolvedHandlers) { resolvedHandlers += resolved }
		synchronized(rejectedHandlers) { rejectedHandlers += rejected }
		flush()
	}

	fun then(c: Continuation<T>) {
		this.then(
			resolved = { c.resume(it) },
			rejected = { c.resumeWithException(it) }
		)
	}

	private val onCancel = Signal<Throwable>()

	override fun cancel(e: Throwable) {
		onCancel(e)
		complete(null, CancellationException())
	}

	suspend fun await(): T = korioSuspendCoroutine(this::then)
}