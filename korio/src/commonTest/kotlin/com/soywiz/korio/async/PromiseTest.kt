package com.soywiz.korio.async

import com.soywiz.klock.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.test.*

class PromiseTest {
    @Test
    fun test() = suspendTest {
        val startTime = DateTime.now()
        delayPromise(200).await()
        val endTime = DateTime.now()
        assertTrue(endTime - startTime >= 200.milliseconds)
    }

    fun delayPromise(timeMs: Int): Promise<Unit> = Promise<Unit> { resolve, reject ->
        launchImmediately(EmptyCoroutineContext) {
            try {
                delay(timeMs.toLong())
                resolve(Unit)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }
}
