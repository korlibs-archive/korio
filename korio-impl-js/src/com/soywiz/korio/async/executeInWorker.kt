package com.soywiz.korio.async

impl suspend fun <T> executeInWorker(callback: suspend () -> T): T {
	return callback()
}