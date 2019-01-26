package com.soywiz.korio.async

import kotlinx.coroutines.*

actual fun asyncEntryPoint(callback: suspend () -> Unit) =
	runBlocking(Dispatchers.Main) { callback() }

