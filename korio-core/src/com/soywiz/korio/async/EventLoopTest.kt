package com.soywiz.korio.async

import com.soywiz.korio.util.compareToChain
import java.io.Closeable
import java.util.*
import kotlin.Comparator

class EventLoopTest : EventLoop {
	var time = 0L

	data class Timer(val time: Long, val handler: () -> Unit) : Comparable<Timer> {
		override fun compareTo(other: Timer): Int = this.time.compareTo(other.time).compareToChain {
			Objects.hash(this).compareTo(Objects.hash(other))
		}
	}

	val timers = TreeSet<Timer>(Comparator<Timer> { a, b -> a.compareTo(b) })

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
		val info = Timer(this.time + ms, callback)
		timers += info
		return Closeable {
			timers -= info
		}
	}

	override fun setImmediate(handler: () -> Unit) {
		handler()
	}

	fun step(ms: Int) {
		time += ms
		//println("$time, $timers")
		while (timers.isNotEmpty()) {
			val first = timers.first()
			if (time >= first.time) {
				timers.remove(first)
				//println("handler!")
				first.handler()
			} else {
				break
			}
		}
	}
}