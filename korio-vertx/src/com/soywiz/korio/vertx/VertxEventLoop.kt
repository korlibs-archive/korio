package com.soywiz.korio.vertx

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.EventLoopFactory
import java.io.Closeable

class VertxEventLoopFactory : EventLoopFactory() {
	override val priority = 500
	override val available = true

	override fun createEventLoop(): EventLoop = VertxEventLoop()
}

class VertxEventLoop : EventLoop() {
	val _vertx = vertx

	override fun setImmediateInternal(handler: () -> Unit) {
		_vertx.runOnContext { handler() }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		val timer = _vertx.setPeriodic(ms.toLong(), {
			callback()
		})
		return Closeable {
			_vertx.cancelTimer(timer)
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		var done = false
		val timer = _vertx.setTimer(ms.toLong()) {
			done = true
			callback()
		}
		return Closeable {
			if (!done) {
				done = true
				_vertx.cancelTimer(timer)
			}
		}
	}
}