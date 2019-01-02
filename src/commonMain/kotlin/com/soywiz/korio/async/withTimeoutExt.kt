package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*

suspend fun <T> withTimeout(time: TimeSpan, block: suspend CoroutineScope.() -> T): T {
	return if (time == TimeSpan.NULL) {
		block(CoroutineScope(coroutineContext))
	} else {
		kotlinx.coroutines.withTimeout(time.millisecondsLong, block)
	}
}
