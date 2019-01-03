package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*

actual fun suspendTest(callback: suspend () -> Unit) {
	runBlocking { callback() }
}

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}


