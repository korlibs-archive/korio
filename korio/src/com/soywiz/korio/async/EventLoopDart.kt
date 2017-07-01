package com.soywiz.korio.async

import com.jtransc.annotation.JTranscAddMembers
import com.jtransc.annotation.JTranscMethodBody
import java.io.Closeable

class EventLoopDartFactory : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopDart()
}

//@JTranscAddImports(target = "dart", value = "dart:async")
class EventLoopDart : EventLoop() {
	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val timer = DartTimer()
		timer.start(ms, callback)
		return timer
	}
}

@JTranscAddMembers(target = "dart", value = "Timer timer;")
class DartTimer : Closeable {
	@JTranscMethodBody(target = "dart", value = "this.timer = new Timer(new Duration(milliseconds: p0), () => p1{% IMETHOD kotlin.jvm.functions.Function0:invoke %}());")
	external fun start(time: Int, callback: () -> Unit): Unit

	@JTranscMethodBody(target = "dart", value = "this.timer.cancel();")
	external override fun close(): Unit
}