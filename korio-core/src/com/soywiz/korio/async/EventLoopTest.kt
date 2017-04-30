package com.soywiz.korio.async

import java.io.Closeable
import java.util.*

class EventLoopFactoryTest : EventLoopFactory() {
	override val available = true
	override val priority: Int = Int.MAX_VALUE - 1000

	override fun createEventLoop(): EventLoop = EventLoopTest()
}

class EventLoopTest : EventLoop() {
	override var time: Long = 0L; private set

	private var tasks = LinkedList<() -> Unit>()
	private val lock = Object()
	private val timers = TreeMap<Long, ArrayList<() -> Unit>>()

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
		val items = synchronized(lock) { timers.getOrPut(this.time + ms) { ArrayList() } }
		items += callback
		return Closeable {
			items -= callback
		}
	}

	var executing = false
	fun executeTasks() {
		if (executing) return
		executing = true
		try {
			var checkTimers = true
			while (synchronized(lock) { tasks.isNotEmpty() || checkTimers }) {
				val task = synchronized(lock) { if (tasks.isNotEmpty()) tasks.removeFirst() else null }
				if (task != null) task()

				val handlers = synchronized(lock) {
					if (timers.isNotEmpty()) {
						val item = timers.firstEntry()
						if (time >= item.key) {
							timers.remove(item.key)
							item.value.toList()
						} else {
							checkTimers = false
							null
						}
					} else {
						checkTimers = false
						null
					}
				}

				if (handlers != null) for (handler in handlers) handler()
			}
		} finally {
			executing = false
		}
	}

	override fun setImmediate(handler: () -> Unit) {
		synchronized(lock) { tasks.add(handler) }
		executeTasks()
	}

	override fun step(ms: Int) {
		time += ms
		executeTasks()
	}
}