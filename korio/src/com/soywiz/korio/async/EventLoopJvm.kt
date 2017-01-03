package com.soywiz.korio.async

import com.soywiz.korio.util.compareToChain
import com.soywiz.korio.util.threadLocal
import java.io.Closeable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

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
