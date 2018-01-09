package com.soywiz.korio.async

import com.soywiz.kds.Queue
import com.soywiz.korio.CancellationException
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.lang.Console
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.util.Cancellable

class Promise<T : Any?> : Cancellable {
	class Deferred<T : Any?> {
		val promise = Promise<T>()
		val onCancel = promise.onCancel
		fun resolve(value: T): Unit = run { promise.complete(value, null) }
		fun reject(error: Throwable): Unit = run { promise.complete(null, error) }
		fun toContinuation(ctx: CoroutineContext): CancellableContinuation<T> {
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
	private val resolvedHandlers = Queue<(T) -> Unit>()
	private val rejectedHandlers = Queue<(Throwable) -> Unit>()

	private fun flush() {
		if (!done) return
		if (error != null) {
			while (true) {
				val handler = synchronized(rejectedHandlers) { if (rejectedHandlers.size != 0) rejectedHandlers.dequeue() else null } ?: break
				handler(error ?: RuntimeException())
			}
		} else {
			while (true) {
				val handler = synchronized(resolvedHandlers) { if (resolvedHandlers.size != 0) resolvedHandlers.dequeue() else null } ?: break
				handler(value as T)
			}
		}
	}

	internal fun complete(value: T?, error: Throwable?): Promise<T> {
		if (!this.done) {
			this.value = value
			this.error = error
			this.done = true

			if (error != null && synchronized(resolvedHandlers) { this.rejectedHandlers.size == 0 } && error !is com.soywiz.korio.CancellationException) {
				if (error !is CancellationException) {
					Console.error("## Not handled Promise exception:")
					error.printStackTrace()
				}
			}

			flush()
		}
		return this
	}

	fun then(resolved: (T) -> Unit) {
		synchronized(resolvedHandlers) { resolvedHandlers.queue(resolved) }
		flush()
	}

	fun always(resolved: () -> Unit) {
		then(
				resolved = { resolved() },
				rejected = { resolved() }
		)
	}

	fun then(resolved: (T) -> Unit, rejected: (Throwable) -> Unit) {
		synchronized(resolvedHandlers) { resolvedHandlers.queue(resolved) }
		synchronized(rejectedHandlers) { rejectedHandlers.queue(rejected) }
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
		complete(null, com.soywiz.korio.CancellationException(""))
	}

	suspend fun await(): T = korioSuspendCoroutine(this::then)
}