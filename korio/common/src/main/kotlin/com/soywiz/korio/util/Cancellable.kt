package com.soywiz.korio.util

interface Cancellable {
	fun cancel(e: Throwable = com.soywiz.korio.CancellationException("")): Unit

	interface Listener {
		fun onCancel(handler: (Throwable) -> Unit): Unit
	}

	companion object {
		operator fun invoke(callback: (Throwable) -> Unit) = object : Cancellable {
			override fun cancel(e: Throwable) = callback(e)
		}
	}
}