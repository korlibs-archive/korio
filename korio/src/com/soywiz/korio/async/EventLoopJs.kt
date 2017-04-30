package com.soywiz.korio.async

import com.jtransc.js.*
import com.soywiz.korio.util.Cancellable
import com.soywiz.korio.util.OS
import java.io.Closeable
import java.util.*

class EventLoopFactoryJs : EventLoopFactory() {
	override val available: Boolean get() = OS.isJs
	override val priority: Int = 2000

	override fun createEventLoop(): EventLoop = EventLoopJs()
}

@Suppress("unused")
class EventLoopJs : EventLoop() {
	val immediateHandlers = LinkedList<() -> Unit>()
	var insideImmediate = false

	override fun setImmediateInternal(handler: () -> Unit) {
		//println("setImmediate")
		immediateHandlers += handler
		if (!insideImmediate) {
			insideImmediate = true
			try {
				while (immediateHandlers.isNotEmpty()) {
					val fhandler = immediateHandlers.removeFirst()
					fhandler()
				}
			} finally {
				insideImmediate = false
			}
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val id = global.call("setTimeout", jsFunctionRaw0 { callback() }, ms)
		//println("setTimeout($ms)")
		return Closeable { global.call("clearTimeout", id) }
	}

	override fun requestAnimationFrameInternal(callback: () -> Unit): Closeable {
		val id = global.call("requestAnimationFrame", jsFunctionRaw0 { callback() })
		//println("setTimeout($ms)")
		return Closeable { global.call("cancelAnimationFrame", id) }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		//println("setInterval($ms)")
		val id = global.call("setInterval", jsFunctionRaw0 { callback() }, ms)
		return Closeable { global.call("clearInterval", id) }
	}
}