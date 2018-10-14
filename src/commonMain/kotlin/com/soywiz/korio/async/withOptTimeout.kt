package com.soywiz.korio.async

import kotlinx.coroutines.*

suspend fun <T> withOptTimeout(ms: Long?, name: String = "timeout", callback: suspend () -> T): T {
	if (ms == null) return callback()
	return withTimeout(ms) { callback() }
}
