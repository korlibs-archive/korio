package com.soywiz.korio.async

import java.util.*

val eventLoopDefaultImpl: EventLoop by lazy {
	ServiceLoader.load(EventLoop::class.java).filter { it.available }.sortedBy { it.priority }.firstOrNull()
		?: throw UnsupportedOperationException("EventLoop implementation not found!")
}
