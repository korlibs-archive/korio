package com.soywiz.korio.async

import kotlin.coroutines.*

@Deprecated("", ReplaceWith("coroutineContext", "kotlin.coroutines.coroutineContext"), level = DeprecationLevel.ERROR)
suspend fun getCoroutineContext() = coroutineContext