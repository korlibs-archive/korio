package com.soywiz.korio.async

import com.soywiz.korio.util.compareToChain
import java.io.Closeable
import java.util.*
import kotlin.Comparator

class EventLoopTest : EventLoop {
	var time = 0L

	val timers = TreeMap<Long, ArrayList<() -> Unit>>()

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