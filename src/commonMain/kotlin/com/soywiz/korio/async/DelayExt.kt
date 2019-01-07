package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlin.coroutines.*

suspend fun delay(time: TimeSpan): Unit = kotlinx.coroutines.delay(time.millisecondsLong)
suspend fun CoroutineContext.delay(time: TimeSpan) = kotlinx.coroutines.delay(time.millisecondsLong)
