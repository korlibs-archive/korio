package com.soywiz.korio.async

import java.io.Closeable
import java.util.*
import java.util.concurrent.Executors

class EventLoopTest : EventLoop {
	var time = 0L

	var workerThread: Thread? = null
	val worker = Executors.newSingleThreadExecutor().apply {
		execute {
			workerThread = Thread.currentThread()
		}
	}
	val timers = TreeMap<Long, ArrayList<() -> Unit>>()

	override val available = true
	override val priority: Int = Int.MAX_VALUE - 1000

	override fun init() {
	}

	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		var cancelled = false
		fun step() {
			setTimeout(ms, {
				if (!cancelled) {
					callback()
					step()
				}
			})
		}
		step()
		return Closeable {
			cancelled = true
		}
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val items = timers.getOrPut(this.time + ms) { ArrayList() }
		items += {
			worker.execute { callback() }
		}
		return Closeable {
			items -= callback
		}
	}

	override fun setImmediate(handler: () -> Unit) {
		worker.execute {
			handler()
		}
	}

	suspend fun waitPending() = asyncFun {
		while (workerThread == null) Thread.sleep(1L)
		if (Thread.currentThread() != workerThread) {
			var pending = true
			worker.execute {
				pending = false
			}
			while (pending) Thread.sleep(1L)
		}
	}

	suspend fun step(ms: Int) = asyncFun {
		waitPending()
		time += ms
		//println("$time, $timers")
		while (timers.isNotEmpty()) {
			timers.firstEntry()
			val (firstTime, firstHandlers) = timers.firstEntry()
			if (time >= firstTime) {
				timers.remove(firstTime)
				//println("handler!")
				worker.execute {
					for (handler in firstHandlers) handler()
				}
			} else {
				break
			}
		}
	}
}