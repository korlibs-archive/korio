package com.soywiz.korio.async

import com.soywiz.kds.LinkedList
import com.soywiz.korio.lang.Closeable

class EventLoopFactoryTest : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopTest()
}

class EventLoopTest : EventLoop(captureCloseables = true) {
	override var time: Long = 0L; private set

	data class Entry(val el: EventLoopTest, val time: Long, val handler: () -> Unit) : Closeable {
		override fun close() {
			el.timers -= this
		}
	}

	private var tasks = LinkedList<() -> Unit>()
	private val lock = Any()
	private val timers = ArrayList<Entry>()

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

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable = synchronized(lock) {
		val entry = Entry(this, this.time + ms, callback)
		timers += entry
		entry
	}

	var executing = false
	fun executeTasks() = synchronized(lock) {
		if (executing) return@synchronized
		executing = true
		try {
			do {
				var count = 0
				if (tasks.isNotEmpty()) {
					tasks.removeFirst()()
					count++
				}

				val stimers = timers.filter { time >= it.time }
				for (entry in stimers) {
					entry.close()
					entry.handler()
					count++
				}
			} while (count > 0)
		} finally {
			executing = false
		}
	}

	override fun setImmediateInternal(handler: () -> Unit) {
		synchronized(lock) { tasks.add(handler) }
		executeTasks()
	}

	override fun step(ms: Int) {
		time += ms
		executeTasks()
	}
}