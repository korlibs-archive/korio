package com.soywiz.korio.vertx

import com.soywiz.korio.async.EventLoop
import java.io.Closeable

class VertxEventLoop : EventLoop() {
	val _vertx = vertx

	override val priority = 500
	override val available = true

	override fun init() {
	}

	override fun setImmediate(handler: () -> Unit) {
		_vertx.runOnContext { handler() }
	}

	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		val timer = _vertx.setPeriodic(ms.toLong(), {
			callback()
		})
		return Closeable {
			_vertx.cancelTimer(timer)
		}
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
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