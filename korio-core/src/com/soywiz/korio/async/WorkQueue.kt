package com.soywiz.korio.async

import kotlin.coroutines.startCoroutine

class WorkQueue {
	private var promise: Promise<Any> = Promise.resolved(Unit)

	operator fun invoke(func: suspend () -> Unit): WorkQueue {
		val oldPromise = this.promise
		val newDeferred = Promise.Deferred<Any>()
		this.promise = newDeferred.promise
		oldPromise.then {
			func.startCoroutine(newDeferred.toContinuation())
		}
		return this
	}
}