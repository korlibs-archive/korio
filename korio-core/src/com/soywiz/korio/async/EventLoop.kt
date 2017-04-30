@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.coroutine.*
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.service.Services
import java.io.Closeable

abstract class EventLoopFactory : Services.Impl() {
	abstract fun createEventLoop(): EventLoop
}

// @TODO: Check CoroutineDispatcher
abstract class EventLoop : Services.Impl() {
	val coroutineContext = EventLoopCoroutineContext(this)

	companion object {
		fun main(eventLoop: EventLoop, entry: suspend EventLoop.() -> Unit): Unit {
			tasksInProgress.incrementAndGet()
			eventLoop.setImmediate {
				entry.korioStartCoroutine(eventLoop, object : Continuation<Unit> {
					override val context: CoroutineContext = EventLoopCoroutineContext(eventLoop)

					override fun resume(value: Unit) {
						tasksInProgress.decrementAndGet()
					}

					override fun resumeWithException(exception: Throwable) {
						tasksInProgress.decrementAndGet()
						exception.printStackTrace()
					}
				})
			}
			eventLoop.loop()
		}

		operator fun invoke(entry: suspend EventLoop.() -> Unit): Unit = main(entry)

		fun main(entry: suspend EventLoop.() -> Unit): Unit = main(eventLoopFactoryDefaultImpl.createEventLoop()) {
			this.entry()
		}
	}

	open fun loop(): Unit = Unit
	abstract fun setTimeout(ms: Int, callback: () -> Unit): Closeable

	open fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			setTimeout(ms, {
				if (!cancelled) {
					step()
					callback()
				}
			})
		}
		step()
		return Closeable { cancelled = true }
	}

	fun setIntervalImmediate(ms: Int, callback: () -> Unit): Closeable {
		setImmediate(callback)
		return setInterval(ms, callback)
	}

	fun queue(handler: () -> Unit): Unit = setImmediate(handler)

	open fun setImmediate(handler: () -> Unit): Unit {
		setTimeout(0, handler)
	}

	open fun requestAnimationFrame(handler: () -> Unit): Unit {
		setTimeout(1000 / 60, handler)
	}

	open val time: Long get() = System.currentTimeMillis()

	open fun step(ms: Int): Unit {
	}


	suspend fun sleep(ms: Int): Unit = suspendCancellableCoroutine { c ->
		val cc = setTimeout(ms) { c.resume(Unit) }
		c.onCancel {
			cc.close()
		}
	}

	suspend fun sleepNextFrame(): Unit = suspendCancellableCoroutine { c ->
		var cancelled = false
		requestAnimationFrame {
			if (!cancelled) c.resume(Unit)
		}
		c.onCancel {
			cancelled = true
		}
	}
}

class EventLoopCoroutineContext(val eventLoop: EventLoop) : AbstractCoroutineContextElement(EventLoopCoroutineContext.Key) {
	companion object Key : CoroutineContextKey<EventLoopCoroutineContext>
}

val CoroutineContext.eventLoop: EventLoop get() {
	return this[EventLoopCoroutineContext.Key]?.eventLoop
		?: invalidOp("No EventLoop associated to this CoroutineContext")
}

val Continuation<*>.eventLoop: EventLoop get() = this.context.eventLoop

suspend fun CoroutineContext.sleep(ms: Int) = this.eventLoop.sleep(ms)