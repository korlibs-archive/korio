package com.soywiz.korio.async

import com.soywiz.korio.service.Services

val eventLoopDefaultImpl: EventLoop by lazy { Services.load<EventLoop>() }
