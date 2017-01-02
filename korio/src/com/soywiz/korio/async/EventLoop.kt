package com.soywiz.korio.async

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.global
import com.jtransc.js.jsFunctionRaw0
import com.jtransc.js.methods
import com.soywiz.korio.util.compareToChain
import com.soywiz.korio.util.threadLocal
import java.io.Closeable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Comparator
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

interface EventLoop {
	companion object {
		@JTranscMethodBody(target = "js", value = "return {% CONSTRUCTOR com.soywiz.korio.async.EventLoop@EventLoopJs:()V %}();")
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
		//val handlers = ConcurrentLinkedDeque<() -> Unit>()
		var timerHandlers = TreeSet<TimerHandler>(Comparator<TimerHandler> { a, b -> if (Objects.equals(a, b)) 0 else a.time.compareTo(b.time).compareToChain { Objects.hash(a).compareTo(Objects.hash(b)) } })

		var eventLoopRunning = AtomicBoolean(false)

		override fun init(): Unit {
			ensureEventLoop()
		}

		private fun ensureEventLoop(): Unit {
			if (!eventLoopRunning.compareAndSet(false, true)) return
			Thread {
				while (timerHandlers.isNotEmpty() || tasksInProgress.get() > 0) {
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

		val immediates by threadLocal { LinkedList<() -> Unit>() }
		var insideImmediate by threadLocal { false }

		override fun setImmediate(handler: () -> Unit) {
			ensureEventLoop()
			immediates += handler
			if (!insideImmediate) {
				insideImmediate = true
				try {
					while (immediates.isNotEmpty()) {
						val immediate = immediates.removeFirst()
						immediate()
					}
				} finally {
					insideImmediate = false
				}
			}
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
		override fun init(): Unit {
		}

		val immediateHandlers = LinkedList<() -> Unit>()
		var insideImmediate = false

		override fun setImmediate(handler: () -> Unit) {
			//println("setImmediate")
			immediateHandlers += handler
			if (!insideImmediate) {
				insideImmediate = true
				try {
					while (immediateHandlers.isNotEmpty()) {
						val handler = immediateHandlers.removeFirst()
						handler()
					}
				} finally {
					insideImmediate = false
				}
			}
		}

		override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
			val id = global.methods["setTimeout"](jsFunctionRaw0 { callback() }, ms)
			//println("setTimeout($ms)")
			return Closeable { global.methods["clearTimeout"](id) }
		}

		override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
			//println("setInterval($ms)")
			val id = global.methods["setInterval"](jsFunctionRaw0 { callback() }, ms)
			return Closeable { global.methods["clearInterval"](id) }
		}
	}
}
