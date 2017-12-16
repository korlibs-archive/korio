package com.soywiz.korio.async

import com.soywiz.korio.coroutine.COROUTINE_SUSPENDED
import com.soywiz.korio.coroutine.Continuation
import com.soywiz.korio.coroutine.getCoroutineContext
import java.lang.reflect.Method

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? {
	val method = this@invokeSuspend
	val cc = getCoroutineContext()

	val lastParam = method.parameterTypes.lastOrNull()
	val margs = java.util.ArrayList(args)
	var deferred: Promise.Deferred<Any?>? = null

	if (lastParam != null && lastParam.isAssignableFrom(Continuation::class.java)) {
		deferred = Promise.Deferred<Any?>()
		margs += deferred.toContinuation(cc)
	}
	val result = method.invoke(obj, *margs.toTypedArray())
	return if (result == COROUTINE_SUSPENDED) {
		deferred?.promise?.await()
	} else {
		result
	}
}