package com.soywiz.korio.async

import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.korioStartCoroutine
import com.soywiz.korio.lang.CancellationException

suspend fun withTimeout(ms: Int, name: String = "timeout", callback: suspend () -> Unit) = suspendCancellableCoroutine<Unit> { c ->
	var cancelled = false
	val timer = c.eventLoop.setTimeout(ms) {
		//c.cancel(TimeoutException())
		c.cancel(CancellationException(""))
		//c.resumeWithException(TimeoutException(name))
	}
	c.onCancel {
		cancelled = true
		timer.close()
		c.cancel()
	}
	callback.korioStartCoroutine(object : Continuation<Unit> {
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
