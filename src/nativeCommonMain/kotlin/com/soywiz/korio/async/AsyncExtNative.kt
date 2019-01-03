package com.soywiz.korio.async

import kotlin.coroutines.*
import kotlinx.coroutines.*

actual fun suspendTest(callback: suspend () -> Unit) =
	runBlocking { callback() }


actual fun asyncEntryPoint(context: CoroutineContext, callback: suspend () -> Unit) =
	runBlocking(context) { callback() }
