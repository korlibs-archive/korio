package com.soywiz.korio.async

val eventLoopDefaultImpl: EventLoop by lazy {
	throw UnsupportedOperationException("EventLoop implementation not found!")
}
