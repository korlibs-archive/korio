package com.soywiz.korio.async

import java.io.Closeable
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timerTask

class EventLoopJvm : EventLoop {
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
