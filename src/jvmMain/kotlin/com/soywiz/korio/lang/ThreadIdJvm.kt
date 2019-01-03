package com.soywiz.korio.lang

actual val currentThreadId: Long get() = Thread.currentThread().id