package com.soywiz.korio.async

import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun withTimeout(ms: Int, name: String = "timeout", callback: suspend () -> Unit) = suspendCancellableCoroutine<Unit> { c ->
	var cancelled = false
	val timer = EventLoop.setTimeout(ms) {
		//c.cancel(TimeoutException())
		c.cancel(CancellationException())
		//c.resumeWithException(TimeoutException(name))
	}
	c.onCancel {
		cancelled = true
		timer.close()
		c.cancel()
	}
	callback.startCoroutine(object : Continuation<Unit> {
		override val context: CoroutineContext = c.context

		override fun resume(value: Unit) {
			if (cancelled) return
			timer.close()
			c.resume(value)
		}

		override fun resumeWithException(exception: Throwable) {
			if (cancelled) return
			timer.close()
			c.resumeWithException(exception)
		}
	})
}
