package com.soywiz.korio.async

import com.soywiz.korio.KorioNative

suspend fun <T> executeInWorker(callback: suspend () -> T): T = KorioNative.executeInWorker(callback)
