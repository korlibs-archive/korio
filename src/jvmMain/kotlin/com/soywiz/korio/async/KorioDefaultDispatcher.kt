package com.soywiz.korio.async

import com.soywiz.kds.*
import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlinx.coroutines.scheduling.*
import kotlinx.coroutines.scheduling.ExperimentalCoroutineDispatcher
import kotlinx.coroutines.timeunit.TimeUnit
import java.io.*
import java.util.concurrent.*
import kotlin.coroutines.*

// @TODO:
actual val KorioDefaultDispatcher: CoroutineDispatcher = newSingleThreadContext("KorioDefaultDispatcher")
//actual val KorioDefaultDispatcher: CoroutineDispatcher = DefaultDispatcher
