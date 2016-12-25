package com.soywiz.coktvfs

import kotlin.coroutines.suspendCoroutine

suspend fun <T> executeInWorker(task: () -> T): T = suspendCoroutine<T> { c ->
    Thread {
        try {
            val result = task()
            c.resume(result)
        } catch (e: Throwable) {
            c.resumeWithException(e)
        }
    }.run()
}