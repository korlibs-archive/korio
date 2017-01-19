package com.soywiz.korio.async

import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.SUSPENDED_MARKER

suspend fun Method.invokeSuspend(obj: Any?, args: List<Any?>): Any? {
	val method = this

	val lastParam = method.parameterTypes.lastOrNull()
	val margs = java.util.ArrayList(args)
	var deferred: Promise.Deferred<Any?>? = null

	if (lastParam != null && lastParam.isAssignableFrom(Continuation::class.java)) {
		deferred = Promise.Deferred<Any?>()
		margs += deferred.toContinuation()
	}
	val result = method.invoke(obj, *margs.toTypedArray())
	return if (result == SUSPENDED_MARKER) {
		deferred?.promise?.await()
	} else {
		result
	}
}
