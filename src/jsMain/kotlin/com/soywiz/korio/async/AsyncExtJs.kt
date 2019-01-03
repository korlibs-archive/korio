package com.soywiz.korio.async

import kotlin.coroutines.*

actual fun suspendTest(callback: suspend () -> Unit): dynamic = kotlin.js.Promise<dynamic> { resolve, reject ->
	callback.startCoroutine(object : Continuation<Unit> {
		override val context: CoroutineContext = KorioDefaultDispatcher
		override fun resumeWith(result: Result<Unit>) {
			val exception = result.exceptionOrNull()
			if (exception != null) {
				reject(exception)
			} else {
				//resolve(undefined)
				resolve(Unit)
			}
		}
	})
}