package com.soywiz.korio.async

import com.soywiz.korio.util.JsMethodBody
import com.soywiz.korio.util.OS
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

open class EventLoop {
	data class TimerHandler(val time: Long, val handler: () -> Unit)

	companion object {
		var impl = EventLoop()

		fun main(eventLoop: EventLoop, entry: suspend () -> Unit) {
			impl = eventLoop
			impl.main(entry)
		}

		fun main(entry: suspend () -> Unit) = impl.main(entry)
		fun queue(handler: () -> Unit) = impl.queue(handler)
		fun step() = impl.step()
		fun setImmediate(handler: () -> Unit) = impl.queue(handler)
		fun setTimeout(ms: Int, callback: () -> Unit): Closeable = impl.setTimeout(ms, callback)
		fun setInterval(ms: Int, callback: () -> Unit): Closeable = impl.setInterval(ms, callback)

		suspend fun sleep(ms: Int): Unit = suspendCoroutine { c ->
			setTimeout(ms) { c.resume(Unit) }
		}
	}

	val handlers = ConcurrentLinkedDeque<() -> Unit>()
	var timerHandlers = ConcurrentLinkedDeque<TimerHandler>()
	var timerHandlersBack = ConcurrentLinkedDeque<TimerHandler>()

	open fun main(entry: suspend () -> Unit = {}): Unit {
		entry.startCoroutine(object : Continuation<Unit> {
			override fun resume(value: Unit) {
			}

			override fun resumeWithException(exception: Throwable) {
				exception.printStackTrace()
			}
		})

		if (!OS.isJs) {
			while (handlers.isNotEmpty() || timerHandlers.isNotEmpty() || Thread.activeCount() > 1) {
				step()
				Thread.sleep(1L)
			}
		}
	}

	@Volatile private var insideStep = false

	@JsMethodBody("")
	open fun step() {
		if (insideStep) return
		insideStep = true
		try {
			while (handlers.isNotEmpty()) {
				val handler = handlers.removeFirst()
				handler?.invoke()
			}
			val now = System.currentTimeMillis()
			while (timerHandlers.isNotEmpty()) {
				val handler = timerHandlers.removeFirst()
				if (now >= handler.time) {
					handler.handler()
				} else {
					timerHandlersBack.add(handler)
				}
			}
			val temp = timerHandlersBack
			timerHandlersBack = timerHandlers
			timerHandlers = temp
		} finally {
			insideStep = false
		}
	}

	@JsMethodBody("""
		var time = p0, handler = p1;
		return setTimeout(function() { handler['{% METHOD kotlin.jvm.functions.Function0:invoke %}'](); }, time);
	""")
	external private fun _setTimeout(time: Int, c: () -> Unit): Double

	@JsMethodBody("""
		var time = p0, handler = p1;
		return setInterval(function() { handler['{% METHOD kotlin.jvm.functions.Function0:invoke %}'](); }, time);
	""")
	external private fun _setInterval(time: Int, c: () -> Unit): Double

	@JsMethodBody("""return clearTimeout(p0);""")
	external private fun _clearTimeout(id: Double): Unit

	@JsMethodBody("""return clearInterval(p0);""")
	external private fun _clearInterval(id: Double): Unit

	open fun queue(handler: () -> Unit) {
		if (OS.isJs) {
			_setTimeout(0, handler)
		} else {
			handlers += handler
			step()
		}
	}

	fun setImmediate(handler: () -> Unit) = queue(handler)

	open fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		if (OS.isJs) {
			val id = _setTimeout(ms, callback)
			return Closeable { _clearTimeout(id) }
		} else {
			val handler = TimerHandler(System.currentTimeMillis() + ms, callback)
			timerHandlers.add(handler)
			return Closeable { timerHandlers.remove(handler) }
		}
	}

	open fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		if (OS.isJs) {
			val id = _setInterval(ms, callback)
			return Closeable { _clearInterval(id) }
		} else {
			var ccallback: (() -> Unit)? = null
			var disposable: Closeable? = null

			ccallback = {
				callback()
				disposable = setTimeout(ms, ccallback!!)
			}

			disposable = setTimeout(ms, ccallback!!)

			return Closeable { disposable?.close() }
		}
	}
}
