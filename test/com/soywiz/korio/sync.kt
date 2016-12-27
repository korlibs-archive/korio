package com.soywiz.korio

import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

// Wait for a suspension block for testing purposes
fun <T> sync(block: suspend () -> T): T {
    var result: Any? = null

    block.startCoroutine(object : Continuation<T> {
        override fun resume(value: T) = run { result = value }
        override fun resumeWithException(exception: Throwable) = run { result = exception }
    })

    while (result == null) Thread.sleep(1L)
    if (result is Throwable) throw result as Throwable
    return result as T
}
