@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.async

import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.CoroutineContext
import com.soywiz.korio.coroutine.EmptyCoroutineContext
import com.soywiz.korio.coroutine.korioStartCoroutine
import com.soywiz.korio.service.Services
import java.io.Closeable

// @TODO: Check CoroutineDispatcher
abstract class EventLoop : Services.Impl() {
	companion object {
		var _impl: EventLoop? = null
		var impl: EventLoop
			set(value) {
				_impl = value
			}
			get() {
				if (_impl == null) _impl = eventLoopDefaultImpl
				return _impl!!
			}

		fun main(eventLoop: EventLoop, entry: suspend () -> Unit): Unit {
			impl = eventLoop
			main(entry)
		}

		fun main(entry: suspend () -> Unit): Unit {
			tasksInProgress.incrementAndGet()
			impl.init()
			impl.setImmediate {
				entry.korioStartCoroutine(object : Continuation<Unit> {
					override val context: CoroutineContext = EmptyCoroutineContext

					override fun resume(value: Unit) {
						tasksInProgress.decrementAndGet()
					}

					override fun resumeWithException(exception: Throwable) {
						tasksInProgress.decrementAndGet()
						exception.printStackTrace()
					}
				})
			}
			impl.loop()
		}

		fun queue(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setImmediate(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
			if (ms <= 0) {
				impl.setImmediate(callback)
				return Closeable { }
			} else {
				return impl.setTimeout(ms, callback)
			}
		}

		fun setInterval(ms: Int, callback: () -> Unit): Closeable = impl.setInterval(ms, callback)
		fun setIntervalImmediate(ms: Int, callback: () -> Unit): Closeable {
			impl.setImmediate(callback)
			return impl.setInterval(ms, callback)
		}

		fun requestAnimationFrame(handler: () -> Unit): Unit = impl.requestAnimationFrame(handler)

		suspend fun sleep(ms: Int): Unit = suspendCancellableCoroutine { c ->
			val cc = setTimeout(ms) { c.resume(Unit) }
			c.onCancel {
				cc.close()
			}
		}

		val time: Long get() = impl.time
		suspend fun sleepNextFrame(): Unit = suspendCancellableCoroutine { c ->
			var cancelled = false
			requestAnimationFrame {
				if (!cancelled) c.resume(Unit)
			}
			c.onCancel {
				cancelled = true
			}
		}

		//suspend fun sleep(ms: Int): Unit = suspendCoroutine { c ->
		//	setTimeout(ms) { c.resume(Unit) }
		//}
	}

	open fun init(): Unit = Unit
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

	open fun setImmediate(handler: () -> Unit): Unit {
		setTimeout(0, handler)
	}

	open fun requestAnimationFrame(handler: () -> Unit): Unit {
		setTimeout(1000 / 60, handler)
	}

	open val time: Long get() = System.currentTimeMillis()

	open fun step(ms: Int): Unit {
	}
}