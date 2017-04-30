package com.soywiz.korio.async

import com.soywiz.korio.service.Services

val eventLoopFactoryDefaultImpl: EventLoopFactory by lazy { Services.load<EventLoopFactory>() }
