package com.soywiz.korio.async

import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedDeque
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

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

		while (handlers.isNotEmpty() || timerHandlers.isNotEmpty() || Thread.activeCount() > 1) {
			step()
			Thread.sleep(1L)
		}
	}

	fun mainLoop() {
		mainLoopInternal()
	}

	protected open fun mainLoopInternal() {

	}

	open fun step() {
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
	}

	open fun queue(handler: () -> Unit) {
		handlers += handler
	}

	fun setImmediate(handler: () -> Unit) = queue(handler)

	open fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val handler = TimerHandler(System.currentTimeMillis() + ms, callback)
		timerHandlers.add(handler)
		return Closeable { timerHandlers.remove(handler) }
	}

	open fun setInterval(ms: Int, callback: () -> Unit): Closeable {
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
