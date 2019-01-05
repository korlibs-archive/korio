package com.soywiz.korio.async

import kotlinx.coroutines.*

// @TODO:
actual val KorioDefaultDispatcher: CoroutineDispatcher by lazy { Dispatchers.Default }
