package com.soywiz.korio.async

class AsyncSemaphore {
	private var available = 0
	private val signal = Signal<Unit>()

	fun release() {
		available++
		signal(Unit)
	}

	suspend fun acquire() {
		while (true) {
			if (available > 0) {
				available--
				return
			} else {
				signal.waitOne()
			}
		}
	}
}