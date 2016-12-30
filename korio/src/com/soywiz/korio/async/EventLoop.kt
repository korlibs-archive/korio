package com.soywiz.korio.async

import com.soywiz.korio.util.JsMethodBody
import com.soywiz.korio.util.Once
import com.soywiz.korio.util.compareToChain
import java.io.Closeable
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Comparator
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

interface EventLoop {
	companion object {
		@JsMethodBody("return {% CONSTRUCTOR com.soywiz.korio.async.EventLoop@EventLoopJs:()V %}();")
		private fun createEventLoop(): EventLoop = EventLoopJvm()

		var impl: EventLoop = createEventLoop()

		fun main(eventLoop: EventLoop, entry: suspend () -> Unit): Unit {
			impl = eventLoop
			main(entry)
		}

		fun main(entry: suspend () -> Unit): Unit {
			tasksInProgress.incrementAndGet()
			impl.init()
			entry.startCoroutine(object : Continuation<Unit> {
				override fun resume(value: Unit) {
					tasksInProgress.decrementAndGet()
				}
				override fun resumeWithException(exception: Throwable) {
					tasksInProgress.decrementAndGet()
					exception.printStackTrace()
				}
			})
		}

		fun queue(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setImmediate(handler: () -> Unit): Unit = impl.setImmediate(handler)
		fun setTimeout(ms: Int, callback: () -> Unit): Closeable = impl.setTimeout(ms, callback)
		fun setInterval(ms: Int, callback: () -> Unit): Closeable = impl.setInterval(ms, callback)

		suspend fun sleep(ms: Int): Unit = suspendCoroutine { c -> setTimeout(ms) { c.resume(Unit) } }
	}

	fun init(): Unit
	fun setInterval(ms: Int, callback: () -> Unit): Closeable
	fun setTimeout(ms: Int, callback: () -> Unit): Closeable
	fun setImmediate(handler: () -> Unit): Unit

	class EventLoopJvm : EventLoop {
		data class TimerHandler(val time: Long, val handler: () -> Unit, val id: Int)

		var lastId = 0
		val handlers = ConcurrentLinkedDeque<() -> Unit>()
		var timerHandlers = TreeSet<TimerHandler>(Comparator<TimerHandler> { a, b -> if (Objects.equals(a, b)) 0 else a.time.compareTo(b.time).compareToChain { Objects.hash(a).compareTo(Objects.hash(b)) } })

		var eventLoopRunning = AtomicBoolean(false)

		override fun init(): Unit {
			ensureEventLoop()
		}

		@JsMethodBody("")
		private fun ensureEventLoop(): Unit {
			if (!eventLoopRunning.compareAndSet(false, true)) return
			Thread {
				while (handlers.isNotEmpty() || timerHandlers.isNotEmpty() || tasksInProgress.get() > 0) {
					//println("step: ${handlers.size} : ${timerHandlers.size} : ${Thread.activeCount()}")
					//println(workerLazyPool.isTerminated)
					//println(workerLazyPool.isShutdown)
					//println(tasksInProgress)
					while (lock { handlers.isNotEmpty() }) {
						val handler = lock { handlers.removeFirst() }
						handler?.invoke()
					}
					val now = System.currentTimeMillis()
					while (lock { timerHandlers.isNotEmpty() }) {
						val handler = lock { timerHandlers.first() }
						if (now >= handler.time) {
							lock { timerHandlers.remove(handler) }
							handler.handler()
						} else {
							break
						}
					}
					Thread.sleep(16L)
				}
				eventLoopRunning.compareAndSet(true, false)
			}.apply {
				isDaemon = true
			}.start()
		}

		override fun setImmediate(handler: () -> Unit) {
			ensureEventLoop()
			lock { handlers += handler }
		}

		override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
			ensureEventLoop()
			val handler = TimerHandler(System.currentTimeMillis() + ms, callback, lastId++)
			lock { timerHandlers.add(handler) }
			return Closeable { lock { timerHandlers.remove(handler) } }
		}


		override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
			ensureEventLoop()
			var ccallback: (() -> Unit)? = null
			var disposable: Closeable? = null

			ccallback = {
				callback()
				disposable = setTimeout(ms, ccallback!!)
			}

			disposable = setTimeout(ms, ccallback!!)

			return Closeable { disposable?.close() }
		}

		inline private fun <T> lock(callback: () -> T) = synchronized(this, callback)
	}

	@Suppress("unused")
	class EventLoopJs : EventLoop {
		@JsMethodBody("""var time = p0, handler = p1;return setTimeout(function() { handler['{% METHOD kotlin.jvm.functions.Function0:invoke %}'](); }, time);""")
		external private fun _setTimeout(time: Int, c: () -> Unit): Double

		@JsMethodBody("""var time = p0, handler = p1;return setInterval(function() { handler['{% METHOD kotlin.jvm.functions.Function0:invoke %}'](); }, time);""")
		external private fun _setInterval(time: Int, c: () -> Unit): Double

		@JsMethodBody("""return clearTimeout(p0);""")
		external private fun _clearTimeout(id: Double): Unit

		@JsMethodBody("""return clearInterval(p0);""")
		external private fun _clearInterval(id: Double): Unit

		override fun init(): Unit {
		}

		override fun setImmediate(handler: () -> Unit) {
			_setTimeout(0, handler)
		}

		override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
			val id = _setTimeout(ms, callback)
			return Closeable { _clearTimeout(id) }
		}

		inline private fun <T> lock(callback: () -> T) = synchronized(this, callback)

		override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
			val id = _setInterval(ms, callback)
			return Closeable { _clearInterval(id) }
		}
	}

}
