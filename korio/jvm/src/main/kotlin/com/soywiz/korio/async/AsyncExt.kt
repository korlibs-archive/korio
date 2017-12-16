package com.soywiz.korio.async

import com.soywiz.korio.coroutine.eventLoop
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

var _workerLazyPool: ExecutorService? = null
val workerLazyPool: ExecutorService by lazy {
	//val pool = Executors.newCachedThreadPool()
	val pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
	_workerLazyPool = pool
	//Runtime.getRuntime().addShutdownHook(object : Thread() {
	//	override fun run() = pool.shutdown()
	//})
	pool
}

fun Executor.executeUpdatingTasksInProgress(action: () -> Unit) {
	tasksInProgress.incrementAndGet()
	this.execute {
		try {
			action()
		} finally {
			tasksInProgress.decrementAndGet()
		}
	}
}

fun <T> Promise<T>.jvmSyncAwait(): T {
	var completed = false
	val lock = Any()
	var error: Throwable? = null
	var result: T? = null

	this.then(resolved = {
		synchronized(lock) {
			completed = true
			result = it
		}
	}, rejected = {
		synchronized(lock) {
			completed = true
			error = it
		}
	})

	while (true) {
		synchronized(lock) {
			if (completed) {
				if (error != null) throw error!!
				if (result != null) return result!!
				throw IllegalStateException()
			}
		}
		Thread.sleep(10)
	}
}

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

suspend private fun <T> _executeInside(task: suspend () -> T, taskRunner: (body: () -> Unit) -> Unit): T {
	val deferred = Promise.Deferred<T>()
	val parentEventLoop = eventLoop()
	tasksInProgress.incrementAndGet()
	taskRunner {
		syncTest {
			try {
				val res = task()
				parentEventLoop.queue {
					deferred.resolve(res)
				}
			} catch (e: Throwable) {
				parentEventLoop.queue { deferred.reject(e) }
			} finally {
				tasksInProgress.decrementAndGet()
			}
		}
	}
	return deferred.promise.await()
}

suspend fun <T> executeInNewThread(task: suspend () -> T): T = _executeInside(task) { body ->
	Thread {
		body()
	}.apply {
		isDaemon = true
		start()
	}
}

suspend fun <T> executeInWorkerJvm(task: suspend () -> T): T = _executeInside(task) { body ->
	workerLazyPool.executeUpdatingTasksInProgress {
		body()
	}
}
