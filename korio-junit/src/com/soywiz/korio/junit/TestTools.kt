package com.soywiz.korio.junit

import com.soywiz.korio.async.EventLoopTest
import com.soywiz.korio.async.sync

fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
	sync(el = EventLoopTest(), step = 10, block = block)
}
