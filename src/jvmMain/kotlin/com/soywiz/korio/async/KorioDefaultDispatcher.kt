package com.soywiz.korio.async

import kotlinx.coroutines.*

// @TODO:
actual val KorioDefaultDispatcher: CoroutineDispatcher = newSingleThreadContext("KorioDefaultDispatcher")
//actual val KorioDefaultDispatcher: CoroutineDispatcher = DefaultDispatcher
