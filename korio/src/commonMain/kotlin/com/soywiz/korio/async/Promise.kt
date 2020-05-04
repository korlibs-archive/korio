package com.soywiz.korio.async

import com.soywiz.korio.experimental.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.js.*

/**
 * An simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@JsName("Promise")
@KorioExperimentalApi
interface Promise<T> {
    @JsName("then")
    fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S>
}

/**
 * An simple interface compatible with JS Promise used for interop. In other cases just use [CompletableDeferred] instead.
 */
@KorioExperimentalApi
expect fun <T> Promise(coroutineContext: CoroutineContext = EmptyCoroutineContext, executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T>

@KorioExperimentalApi
suspend fun <T> SPromise(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit): Promise<T> = Promise(coroutineContext, executor)

internal class DeferredPromise<T>(
    internal val completableDeferred: CompletableDeferred<T>,
    val coroutineContext: CoroutineContext = EmptyCoroutineContext
) : Promise<T> {
    val deferred: Deferred<T> get() = completableDeferred
    override fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S> {
        val chainedPromise = DeferredPromise(CompletableDeferred<S>(), coroutineContext)

        launchImmediately(coroutineContext) {
            var result: S? = null
            try {
                result = onFulfilled?.invoke(completableDeferred.await())
            } catch (e: Throwable) {
                result = onRejected?.invoke(e)
            }
            if (result != null) {
                chainedPromise.completableDeferred.complete(result)
            }
        }

        return chainedPromise
    }
}

fun <T> CompletableDeferred<T>.toPromise(coroutineContext: CoroutineContext = EmptyCoroutineContext): Promise<T> = DeferredPromise(this, coroutineContext)
suspend fun <T> CompletableDeferred<T>.toPromise(): Promise<T> = toPromise(coroutineContext)

fun <T> Promise<T>.toDeferred(): Deferred<T> {
    val out = CompletableDeferred<T>()
    this.then({ out.complete(it) }, { out.completeExceptionally(it) })
    return out
}

suspend fun <T> Promise<T>.await(): T = suspendCoroutine { c ->
    this.then({ c.resume(it) }, { c.resumeWithException(it) })
}
