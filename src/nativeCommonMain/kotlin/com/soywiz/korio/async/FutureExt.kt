package com.soywiz.korio.async

suspend fun <T> kotlin.native.concurrent.Future<T>.await(): T {
	var n = 0
	while (this.state != kotlin.native.concurrent.FutureState.COMPUTED) {
		if (this.state == kotlin.native.concurrent.FutureState.INVALID) error("Error in worker")
		if (this.state == kotlin.native.concurrent.FutureState.CANCELLED) kotlinx.coroutines.CancellationException("cancelled")
		kotlinx.coroutines.delay(((n++).toDouble() / 3.0).toLong())
	}
	return this.result
}
