package com.soywiz.korio.async

import com.soywiz.korio.coroutine.COROUTINE_SUSPENDED
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.withCoroutineContext
import java.lang.reflect.Method

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? = withCoroutineContext {
	val method = this@invokeSuspend

	val lastParam = method.parameterTypes.lastOrNull()
	val margs = java.util.ArrayList(args)
	var deferred: Promise.Deferred<Any?>? = null

	if (lastParam != null && lastParam.isAssignableFrom(Continuation::class.java)) {
		deferred = Promise.Deferred<Any?>()
		margs += deferred.toContinuation(this)
	}
	val result = method.invoke(obj, *margs.toTypedArray())
	return@withCoroutineContext if (result == COROUTINE_SUSPENDED) {
		deferred?.promise?.await()
	} else {
		result
	}
}