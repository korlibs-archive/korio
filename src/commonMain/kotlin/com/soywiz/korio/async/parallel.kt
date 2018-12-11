package com.soywiz.korio.async

import kotlinx.coroutines.*

object ParallelContext

suspend fun CoroutineScope.parallel(callback: ParallelContext.() -> Unit) {
	val res = launch(KorioDefaultDispatcher) { callback(ParallelContext) }
	res.join()
}

fun CoroutineScope.sequence(callback: suspend ParallelContext.() -> Unit) {
	async(KorioDefaultDispatcher) { callback(ParallelContext) }
}

