package com.soywiz.korio.async

import com.soywiz.korio.util.compareToChain
import java.io.Closeable
import java.util.*
import java.util.concurrent.TimeUnit

class EventLoopFactoryJvmAndCSharp : EventLoopFactory() {
	override val priority: Int = 1000
	override val available: Boolean get() = true

	override fun createEventLoop(): EventLoop = EventLoopJvmAndCSharp()
}

class EventLoopJvmAndCSharp : EventLoop() {
	class Task(val time: Long, val callback: () -> Unit)

	private val lock = Object()

	private val timedTasks = PriorityQueue<Task>(128, { a, b ->
		if (a == b) 0 else a.time.compareTo(b.time).compareToChain { if (a == b) 0 else -1 }
	})

	private val immediateTasks = LinkedList<() -> Unit>()

	override fun setImmediateInternal(handler: () -> Unit) {
		synchronized(lock) { immediateTasks += handler }
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val task = Task(System.currentTimeMillis() + ms, callback)
		synchronized(lock) { timedTasks += task }
		return Closeable { synchronized(timedTasks) { timedTasks -= task } }
	}

	override fun step(ms: Int) {
		timer@ while (true) {
			val startTime = System.currentTimeMillis()
			while (true) {
				val currentTime = System.currentTimeMillis()
				val item = synchronized(lock) { if (timedTasks.isNotEmpty() && currentTime >= timedTasks.peek().time) timedTasks.remove() else null } ?: break
				item.callback()
			}
			while (true) {
				if ((System.currentTimeMillis() - startTime) >= 50) {
					continue@timer
				}
				val task = synchronized(lock) { if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null } ?: break
				task()
			}
			break
		}
	}

	override fun loop() {
		while (synchronized(lock) { immediateTasks.isNotEmpty() || timedTasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			step(1)
			Thread.sleep(1L)
			//println("immediateTasks: ${immediateTasks.size}, timedTasks: ${timedTasks.size}, tasksInProgress: ${tasksInProgress.get()}")
		}
		//_workerLazyPool?.shutdown()
		//_workerLazyPool?.awaitTermination(5, TimeUnit.SECONDS);
		_workerLazyPool?.shutdownNow()
	}
}


//class EventLoopJvmAndCSharp : EventLoop() {
//	override val priority: Int = 1000
//	override val available: Boolean get() = true
//
//	val tasksExecutor = Executors.newSingleThreadExecutor()
//
//	val timer = Timer(true)
//
//	override fun init(): Unit = Unit
//
//	override fun setImmediate(handler: () -> Unit) {
//		tasksExecutor { handler() }
//	}
//
//	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
//		val tt = timerTask { tasksExecutor { callback() } }
//		timer.schedule(tt, ms.toLong())
//		return Closeable { tt.cancel() }
//	}
//
//
//	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
//		val tt = timerTask { tasksExecutor { callback() } }
//		timer.schedule(tt, ms.toLong(), ms.toLong())
//		return Closeable { tt.cancel() }
//	}
//}
