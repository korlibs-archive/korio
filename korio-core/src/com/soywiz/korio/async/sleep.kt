package com.soywiz.korio.async

suspend fun sleep(ms: Int) = EventLoop.sleep(ms)

suspend fun sleepNextFrame() = EventLoop.sleepNextFrame()