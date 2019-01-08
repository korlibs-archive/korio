package com.soywiz.korio.async

import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlin.coroutines.*


// @TODO: Kotlin.JS bug!
//fun suspendTestExceptJs(callback: suspend () -> Unit) = suspendTest {
//	if (OS.isJs) return@suspendTest
//	callback()
//}

private fun CoroutineScope._launch(start: CoroutineStart, callback: suspend () -> Unit): Job = launch(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		e.printStackTrace()
		throw e
	}
}

private fun <T> CoroutineScope._async(start: CoroutineStart, callback: suspend () -> T): Deferred<T> = async(coroutineContext, start = start) {
	try {
		callback()
	} catch (e: CancellationException) {
		throw e
	} catch (e: Throwable) {
		e.printStackTrace()
		throw e
	}
}

fun CoroutineScope.launchImmediately(callback: suspend () -> Unit) = _launch(CoroutineStart.UNDISPATCHED, callback)
fun CoroutineScope.launchAsap(callback: suspend () -> Unit) = _launch(CoroutineStart.DEFAULT, callback)
fun <T> CoroutineScope.asyncImmediately(callback: suspend () -> T) = _async(CoroutineStart.UNDISPATCHED, callback)
fun <T> CoroutineScope.asyncAsap(callback: suspend () -> T) = _async(CoroutineStart.DEFAULT, callback)


fun launchImmediately(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchImmediately(callback)
fun launchAsap(context: CoroutineContext, callback: suspend () -> Unit) = CoroutineScope(context).launchAsap(callback)
fun <T> asyncImmediately(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncImmediately(callback)
fun <T> asyncAsap(context: CoroutineContext, callback: suspend () -> T) = CoroutineScope(context).asyncAsap(callback)

expect fun asyncEntryPoint(callback: suspend () -> Unit)
fun suspendTest(callback: suspend () -> Unit) = asyncEntryPoint(callback)
fun suspendTest(context: CoroutineContext, callback: suspend () -> Unit) = suspendTest { withContext(context) { callback() } }
fun suspendTestExceptJs(callback: suspend () -> Unit) {
	if (!OS.isJs) {
		suspendTest {
			callback()
		}
	}
}

