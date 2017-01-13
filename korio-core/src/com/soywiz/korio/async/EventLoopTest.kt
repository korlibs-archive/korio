package com.soywiz.korio.async

import java.io.Closeable
import java.util.*

class EventLoopTest : EventLoop {
	var time = 0L

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
		items += callback
		return Closeable {
			items -= callback
		}
	}

	override fun setImmediate(handler: () -> Unit) {
		handler()
	}

	fun step(ms: Int) {
		time += ms
		//println("$time, $timers")
		while (timers.isNotEmpty()) {
			timers.firstEntry()
			val (firstTime, firstHandlers) = timers.firstEntry()
			if (time >= firstTime) {
				timers.remove(firstTime)
				//println("handler!")
				for (handler in firstHandlers) handler()
			} else {
				break
			}
		}
	}
}