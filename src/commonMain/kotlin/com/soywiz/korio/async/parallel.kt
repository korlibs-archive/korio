package com.soywiz.korio.async

import kotlinx.coroutines.*

object ParallelContext

suspend fun parallel(callback: ParallelContext.() -> Unit) {
	val res = launch(KorioDefaultDispatcher, parent = Job()) { callback(ParallelContext) }
	res.join()
}

fun ParallelContext.sequence(callback: suspend ParallelContext.() -> Unit) {
	async(KorioDefaultDispatcher) { callback(ParallelContext) }
}

