package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.lang.reflect.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? {
	val method = this@invokeSuspend
	val cc = coroutineContext

	val lastParam = method.parameterTypes.lastOrNull()
	val margs = java.util.ArrayList(args)
	var deferred: CompletableDeferred<Any?>? = null

	if (lastParam != null && lastParam.isAssignableFrom(Continuation::class.java)) {
		deferred = CompletableDeferred<Any?>(Job())
		margs += deferred.toContinuation(cc)
	}
	val result = method.invoke(obj, *margs.toTypedArray())
	return if (result == COROUTINE_SUSPENDED) {
		deferred?.await()
	} else {
		result
	}
}
