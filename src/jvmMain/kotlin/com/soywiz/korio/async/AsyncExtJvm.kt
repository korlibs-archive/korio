package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import kotlin.coroutines.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}

actual fun suspendTest(callback: suspend () -> Unit) {
	runBlocking { callback() }
}

actual fun asyncEntryPoint(context: CoroutineContext, callback: suspend () -> Unit) =
	runBlocking(context) { callback() }
