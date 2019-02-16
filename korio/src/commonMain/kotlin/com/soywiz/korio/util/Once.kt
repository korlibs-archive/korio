package com.soywiz.korio.util

import com.soywiz.korio.async.*
import kotlinx.coroutines.*

class Once {
	var completed = false

	inline operator fun invoke(callback: () -> Unit) {
		if (!completed) {
			completed = true
			callback()
		}
	}
}

class AsyncOnce<T> {
	var promise: Deferred<T>? = null

	suspend operator fun invoke(callback: suspend () -> T): T {
		supervisorScope {
			if (promise == null) {
				promise = asyncImmediately { callback() }
			}
		}
		return promise!!.await()
	}
}
