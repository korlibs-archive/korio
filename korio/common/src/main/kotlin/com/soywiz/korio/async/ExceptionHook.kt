package com.soywiz.korio.async

import com.soywiz.korio.lang.Console
import com.soywiz.korio.lang.printStackTrace

object ExceptionHook {
	var show = false

	fun <T : Throwable> hook(exception: T): T {
		if (show) {
			Console.error("ExceptionHook: $exception")
			exception.printStackTrace()
		}
		return exception
	}
}