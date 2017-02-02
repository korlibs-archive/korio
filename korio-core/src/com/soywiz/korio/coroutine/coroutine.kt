package com.soywiz.korio.coroutine

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.createCoroutine
import kotlin.coroutines.experimental.startCoroutine

//val COROUTINE_SUSPENDED = kotlin.coroutines.experimental.COROUTINE_SUSPENDED
val COROUTINE_SUSPENDED = kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

typealias RestrictsSuspension = kotlin.coroutines.experimental.RestrictsSuspension
typealias Continuation<T> = kotlin.coroutines.experimental.Continuation<T>
typealias CoroutineContext = kotlin.coroutines.experimental.CoroutineContext
typealias CoroutineContextKey<T> = kotlin.coroutines.experimental.CoroutineContext.Key<T>
typealias EmptyCoroutineContext = kotlin.coroutines.experimental.EmptyCoroutineContext
typealias AbstractCoroutineContextElement = kotlin.coroutines.experimental.AbstractCoroutineContextElement

inline suspend fun <T> korioSuspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T = kotlin.coroutines.experimental.suspendCoroutine(block)

fun <R, T> (suspend R.() -> T).korioStartCoroutine(receiver: R, completion: Continuation<T>) = this.startCoroutine(receiver, completion)
fun <T> (suspend () -> T).korioStartCoroutine(completion: Continuation<T>) = this.startCoroutine(completion)
fun <T> (suspend () -> T).korioCreateCoroutine(completion: Continuation<T>): Continuation<Unit> = this.createCoroutine(completion)
fun <R, T> (suspend R.() -> T).korioCreateCoroutine(receiver: R, completion: Continuation<T>): Continuation<Unit> = this.createCoroutine(receiver, completion)
