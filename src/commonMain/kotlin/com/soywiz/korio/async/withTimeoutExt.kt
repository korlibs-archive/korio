package com.soywiz.korio.async

import com.soywiz.klock.TimeSpan
import kotlinx.coroutines.CoroutineScope

public suspend fun <T> withTimeout(time: TimeSpan, block: suspend CoroutineScope.() -> T): T =
	kotlinx.coroutines.withTimeout(time.millisecondsLong, block)
