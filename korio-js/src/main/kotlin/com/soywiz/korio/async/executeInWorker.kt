package com.soywiz.korio.async

actual suspend fun <T> executeInWorker(callback: suspend () -> T): T {
	return callback()
}