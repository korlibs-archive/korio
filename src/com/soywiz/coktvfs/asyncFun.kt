package com.soywiz.coktvfs

import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine

inline suspend fun <T> asyncFun(routine: suspend () -> T): T = suspendCoroutine<T> { routine.startCoroutine(it) }
