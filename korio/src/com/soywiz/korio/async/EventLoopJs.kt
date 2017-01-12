package com.soywiz.korio.async

import com.jtransc.js.global
import com.jtransc.js.jsFunctionRaw0
import com.jtransc.js.methods
import com.soywiz.korio.util.OS
import java.io.Closeable
import java.util.*

@Suppress("unused")
class EventLoopJs : EventLoop {
	override val available: Boolean get() = OS.isJs

	override fun init(): Unit {
	}

	val immediateHandlers = LinkedList<() -> Unit>()
	var insideImmediate = false

	override fun setImmediate(handler: () -> Unit) {
		//println("setImmediate")
		immediateHandlers += handler
		if (!insideImmediate) {
			insideImmediate = true
			try {
				while (immediateHandlers.isNotEmpty()) {
					val handler = immediateHandlers.removeFirst()
					handler()
				}
			} finally {
				insideImmediate = false
			}
		}
	}

	override fun setTimeout(ms: Int, callback: () -> Unit): Closeable {
		val id = global.methods["setTimeout"](jsFunctionRaw0 { callback() }, ms)
		//println("setTimeout($ms)")
		return Closeable { global.methods["clearTimeout"](id) }
	}

	override fun setInterval(ms: Int, callback: () -> Unit): Closeable {
		//println("setInterval($ms)")
		val id = global.methods["setInterval"](jsFunctionRaw0 { callback() }, ms)
		return Closeable { global.methods["clearInterval"](id) }
	}
}