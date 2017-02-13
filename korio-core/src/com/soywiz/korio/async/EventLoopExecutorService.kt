package com.soywiz.korio.async

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

object EventLoopExecutorService : ExecutorService {
	var shutdown = false

	override fun shutdown() = run { shutdown = true }
	override fun shutdownNow(): MutableList<Runnable> = arrayListOf<Runnable>().apply { shutdown() }
	override fun isShutdown(): Boolean = shutdown

	override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun submit(task: Runnable?): Future<*> = submit(task, Unit)
	override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean = true

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun isTerminated(): Boolean = true

	override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): MutableList<Future<T>> {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun execute(command: Runnable) {
		EventLoop.setImmediate { command.run() }
	}
}