package com.soywiz.korio.async

import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.lang.Closeable

class EventLoopFactoryTest : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopTest()
}

class EventLoopTest : EventLoop() {
	override var time: Long = 0L; private set

	private var tasks = LinkedList<() -> Unit>()
	private val lock = Any()
	private val timers = HashMap<Long, ArrayList<() -> Unit>>()

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
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
		return Closeable {
			cancelled = true
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
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
						val item = timers.entries.first()
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

	override fun setImmediateInternal(handler: () -> Unit) {
		synchronized(lock) { tasks += handler }
		executeTasks()
	}

	override fun step(ms: Int) {
		time += ms
		executeTasks()
	}
}