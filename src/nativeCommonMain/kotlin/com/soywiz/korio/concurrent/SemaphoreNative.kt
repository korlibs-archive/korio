package com.soywiz.korio.concurrent

actual class Semaphore actual constructor(initial: Int) {
	actual fun acquire(): Unit = Unit
	actual fun release(): Unit = Unit
}
