package com.soywiz.korio.async

import kotlinx.coroutines.*

actual fun suspendTest(callback: suspend () -> Unit) {
	runBlocking { callback() }
}
