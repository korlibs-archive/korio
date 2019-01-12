package com.soywiz.korio.async

/*
class EventLoopFactoryJvmAndCSharp : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopJvmAndCSharp()
}

class ConcurrentSignal {
	private val lock = java.lang.Object()

	fun sleep(): Unit = run { synchronized2(lock) { lock.wait() } }
	fun sleep(timeout: Long): Unit = run { synchronized2(lock) { lock.wait(timeout) } }
	fun wake(): Unit = run { synchronized2(lock) { lock.notifyAll() } }
}

class EventLoopJvmAndCSharp : EventLoop(captureCloseables = false) {
	class Task(val time: Long, val callback: () -> Unit)

	private val lock = java.lang.Object()
	val useLock = false
	private val slock = ConcurrentSignal()

	private val timedTasks = PriorityQueue<Task>(128, { a, b ->
		if (a == b) 0 else a.time.compareTo(b.time).compareToChain { if (a == b) 0 else -1 }
	})

	class ImmediateTask {
		var continuation: Continuation<*>? = null
		var continuationResult: Any? = null
		var continuationException: Throwable? = null
		var callback: (() -> Unit)? = null

		fun reset() {
			continuation = null
			continuationResult = null
			continuationException = null
			callback = null
		}
	}

	private val immediateTasksPool = Pool({ it.reset() }) { ImmediateTask() }
	private val immediateTasks = Deque<ImmediateTask>()

	override fun setImmediateInternal(handler: () -> Unit) {
		synchronized2(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.callback = handler
			}
		}
		if (useLock) slock.wake()
	}

	override fun <T> queueContinuation(continuation: Continuation<T>, result: T): Unit {
		synchronized2(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationResult = result
			}
		}
		if (useLock) slock.wake()
	}

	override fun <T> queueContinuationException(continuation: Continuation<T>, result: Throwable): Unit {
		synchronized2(lock) {
			immediateTasks += immediateTasksPool.alloc().apply {
				this.continuation = continuation
				this.continuationException = result
			}
		}
		if (useLock) slock.wake()
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val task = Task(System.currentTimeMillis() + ms, callback)
		synchronized2(lock) { timedTasks += task }
		return Closeable { synchronized2(timedTasks) { timedTasks -= task } }
	}

	override fun step(ms: Int) {
		timer@ while (true) {
			val startTime = System.currentTimeMillis()
			while (true) {
				val currentTime = System.currentTimeMillis()
				val item =
					synchronized2(lock) { if (timedTasks.isNotEmpty() && currentTime >= timedTasks.peek().time) timedTasks.remove() else null }
							?: break
				item.callback()
			}
			while (true) {
				if ((System.currentTimeMillis() - startTime) >= 50) {
					continue@timer
				}
				val task =
					synchronized2(lock) { if (immediateTasks.isNotEmpty()) immediateTasks.removeFirst() else null }
							?: break
				if (task.callback != null) {
					task.callback?.invoke()
				} else if (task.continuation != null) {
					val cont = (task.continuation as? Continuation<Any?>)!!
					if (task.continuationException != null) {
						cont.resumeWithException(task.continuationException!!)
					} else {
						cont.resume(task.continuationResult)
					}
				}
				synchronized2(lock) {
					immediateTasksPool.free(task)
				}
			}
			break
		}
	}

	var loopThread: Thread? = null

	override fun loop() {
		loopThread = Thread.currentThread()

		while (synchronized2(lock) { immediateTasks.isNotEmpty() || timedTasks.isNotEmpty() } || (tasksInProgress.get() != 0)) {
			step(1)
			if (useLock) {
				if (synchronized2(lock) { immediateTasks.isEmpty() }) {
					slock.sleep(1L)
				}
			} else {
				Thread.sleep(1L)
			}

			//println("immediateTasks: ${immediateTasks.size}, timedTasks: ${timedTasks.size}, tasksInProgress: ${tasksInProgress.get()}")
		}

		//_workerLazyPool?.shutdownNow()
		//_workerLazyPool?.shutdown()
		//_workerLazyPool?.awaitTermination(5, TimeUnit.SECONDS);
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
*/
