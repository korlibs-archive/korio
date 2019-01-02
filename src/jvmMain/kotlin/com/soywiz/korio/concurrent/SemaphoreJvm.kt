package com.soywiz.korio.concurrent

actual class Semaphore actual constructor(initial: Int) {
	val jsema = java.util.concurrent.Semaphore(initial)
	//var initial: Int
	actual fun acquire() = jsema.acquire()

	actual fun release() = jsema.release()
}
