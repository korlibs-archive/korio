@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.KorioNative
import com.soywiz.korio.coroutine.*
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.AtomicInteger
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.time.TimeProvider

abstract class EventLoopFactory {
	abstract fun createEventLoop(): EventLoop
}

val eventLoopFactoryDefaultImpl: EventLoopFactory get() = KorioNative.eventLoopFactoryDefaultImpl

val tasksInProgress = AtomicInteger(0)

// @TODO: Check CoroutineDispatcher
abstract class EventLoop : Closeable {
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

	abstract protected fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable

	open protected fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			setTimeoutInternal(ms, {
				if (!cancelled) {
					callback()
					step()
				}
			})
		}
		step()
		return Closeable { cancelled = true }
	}

	open protected fun setImmediateInternal(handler: () -> Unit): Unit = run { setTimeoutInternal(0, handler) }

	open protected fun requestAnimationFrameInternal(callback: () -> Unit): Closeable = setTimeoutInternal(1000 / 60, callback)

	open fun loop(): Unit = Unit

	private val closeables = LinkedHashSet<Closeable>()

	private fun Closeable.capture(): Closeable {
		val closeable = this
		closeables += closeable
		return Closeable {
			closeables -= closeable
			closeable.close()
		}
	}

	fun setImmediate(handler: () -> Unit): Unit = setImmediateInternal(handler)
	fun setTimeout(ms: Int, callback: () -> Unit): Closeable = setTimeoutInternal(ms, callback).capture()
	fun setInterval(ms: Int, callback: () -> Unit): Closeable = setIntervalInternal(ms, callback).capture()

	fun setIntervalImmediate(ms: Int, callback: () -> Unit): Closeable {
		setImmediateInternal(callback)
		return setIntervalInternal(ms, callback).capture()
	}

	fun requestAnimationFrame(callback: () -> Unit): Closeable {
		return requestAnimationFrameInternal(callback).capture()
	}

	fun queue(handler: () -> Unit): Unit = setImmediate(handler)

	fun animationFrameLoop(callback: () -> Unit): Closeable {
		var closeable: Closeable? = null
		var step: (() -> Unit)? = null
		var cancelled = false
		step = {
			//println("animationFrameLoop:cancelled:$cancelled")
			if (!cancelled) {
				//println("--callback[")
				callback()
				//println("--callback]")
				closeable = this.requestAnimationFrameInternal(step!!)
			} else {
				//println("--cancelled!")
			}
		}
		step()
		return Closeable {
			cancelled = true
			closeable?.close()
		}.capture()
	}

	override fun close() {
		for (closeable in closeables) {
			closeable.close()
		}
		closeables.clear()
	}

	open val time: Long get() = TimeProvider.now()

	open fun step(ms: Int): Unit = Unit

	suspend fun sleep(ms: Int): Unit = suspendCancellableCoroutine { c ->
		val cc = setTimeout(ms) { c.resume(Unit) }
		c.onCancel { cc.close() }
	}

	suspend fun sleepNextFrame(): Unit = suspendCancellableCoroutine { c ->
		val cc = requestAnimationFrame { c.resume(Unit) }
		c.onCancel { cc.close() }
	}
}

class EventLoopCoroutineContext(val eventLoop: EventLoop) : AbstractCoroutineContextElement(EventLoopCoroutineContext.Key) {
	companion object Key : CoroutineContextKey<EventLoopCoroutineContext>
}

val CoroutineContext.eventLoop: EventLoop
	get() {
		return this[EventLoopCoroutineContext.Key]?.eventLoop
			?: invalidOp("No EventLoop associated to this CoroutineContext")
	}

val Continuation<*>.eventLoop: EventLoop get() = this.context.eventLoop

suspend fun CoroutineContext.sleep(ms: Int) = this.eventLoop.sleep(ms)