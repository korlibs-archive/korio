package com.soywiz.korio

import com.soywiz.korio.async.EventLoop

fun Korio(entry: suspend EventLoop.() -> Unit) = EventLoop.main(entry)

object Korio {
	val VERSION = KORIO_VERSION
}
