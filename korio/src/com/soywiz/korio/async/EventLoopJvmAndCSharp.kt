package com.soywiz.korio.async

import com.soywiz.korio.util.compareToChain
import java.io.Closeable
import java.util.*

class EventLoopJvmAndCSharp : EventLoop() {
	override val priority: Int = 1000
	override val available: Boolean get() = true

	class Task(val time: Long, val callback: () -> Unit)

	val tasks = PriorityQueue<Task>(128, { a, b ->
		if (a == b) 0 else a.time.compareTo(b.time).compareToChain { if (a == b) 0 else -1 }
	})

	override fun init(): Unit = Unit

	override fun setImmediate(handler: () -> Unit) {
		synchronized(tasks) { tasks += Task(0L, handler) }
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val task = Task(System.currentTimeMillis() + ms, callback)
		synchronized(tasks) { tasks += task }
		return Closeable { synchronized(tasks) { tasks -= task } }
	}

	override fun step(ms: Int) {
		val currentTime = System.currentTimeMillis()
		while (true) {
			val item = synchronized(tasks) { if (tasks.isNotEmpty()) tasks.peek() else null } ?: break
			if (currentTime < item.time) break
			synchronized(tasks) { tasks.remove() }
			item.callback()
			continue
		}
	}

	override fun loop() {
		while (synchronized(tasks) { tasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			step(1)
			Thread.sleep(1L)
		}
	}
}

/*
class EventLoopJvm : EventLoop() {
	override val priority: Int = 1000
	override val available: Boolean get() = true

	val tasksExecutor = Executors.newSingleThreadExecutor()

	val timer = Timer(true)

	override fun init(): Unit = Unit

	override fun setImmediate(handler: () -> Unit) {
		tasksExecutor { handler() }
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val tt = timerTask { tasksExecutor { callback() } }
		timer.schedule(tt, ms.toLong())
		return Closeable { tt.cancel() }
	}


	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		val tt = timerTask { tasksExecutor { callback() } }
		timer.schedule(tt, ms.toLong(), ms.toLong())
		return Closeable { tt.cancel() }
	}
}
*/
