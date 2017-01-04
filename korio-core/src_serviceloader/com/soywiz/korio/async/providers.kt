package com.soywiz.korio.async

import java.util.*

val eventLoopDefaultImpl: EventLoop by lazy {
	ServiceLoader.load(EventLoop::class.java).firstOrNull()
		?: throw UnsupportedOperationException("EventLoop implementation not found!")
}
