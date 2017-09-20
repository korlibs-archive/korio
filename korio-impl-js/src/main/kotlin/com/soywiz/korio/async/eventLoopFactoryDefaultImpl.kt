package com.soywiz.korio.async

import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.lang.Closeable
import kotlin.browser.window

impl val eventLoopFactoryDefaultImpl: EventLoopFactory = EventLoopFactoryJs()

class EventLoopFactoryJs : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopJs()
}

val global = window

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
		val id = window.setTimeout({ callback() }, ms)
		//println("setTimeout($ms)")
		return Closeable { global.clearInterval(id) }
	}

	override fun requestAnimationFrameInternal(callback: () -> Unit): Closeable {
		val id = global.requestAnimationFrame { callback() }
		//println("setTimeout($ms)")
		return Closeable { global.cancelAnimationFrame(id) }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		//println("setInterval($ms)")
		val id = global.setInterval({ callback() }, ms)
		return Closeable { global.clearInterval(id) }
	}
}