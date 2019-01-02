package com.soywiz.korio.concurrent

actual class Semaphore actual constructor(initial: Int) {
	//var initial: Int
	actual fun acquire() = Unit

	actual fun release() = Unit
}

