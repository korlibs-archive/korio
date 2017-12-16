package com.soywiz.korio.async

import com.soywiz.korio.KorioNative
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

@Deprecated("")
suspend fun <T> executeInNewThread(task: suspend () -> T): T = KorioNative.executeInNewThread(task)

@Deprecated("")
suspend fun <T> executeInWorkerJvm(task: suspend () -> T): T = KorioNative.executeInWorker(task)
