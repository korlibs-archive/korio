package com.soywiz.korio.async

suspend fun <T> kotlin.native.concurrent.Future<T>.await(): T {
	var n = 0
	while (this.state != kotlin.native.concurrent.FutureState.COMPUTED && this.state != kotlin.native.concurrent.FutureState.INVALID && this.state != kotlin.native.concurrent.FutureState.CANCELLED) {
		kotlinx.coroutines.delay(((n++).toDouble() / 3.0).toLong())
	}
	return this.result
}
