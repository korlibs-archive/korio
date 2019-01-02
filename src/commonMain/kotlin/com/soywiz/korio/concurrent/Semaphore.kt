package com.soywiz.korio.concurrent

expect class Semaphore(initial: Int) {
	//var initial: Int
	fun acquire()

	fun release()
}
