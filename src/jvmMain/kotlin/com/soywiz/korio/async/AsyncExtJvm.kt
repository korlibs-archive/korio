package com.soywiz.korio.async

import com.soywiz.korio.*
import kotlinx.coroutines.*
import java.util.concurrent.*

fun <T> Deferred<T>.jvmSyncAwait(): T = runBlocking { await() }

operator fun ExecutorService.invoke(callback: () -> Unit) {
	this.execute(callback)
}


