package com.soywiz.korio.async

import com.soywiz.korio.lang.Console
import com.soywiz.korio.lang.printStackTrace

object ExceptionHook {
	fun <T : Throwable> hook(exception: T): T {
		Console.error("ExceptionHook: $exception")
		exception.printStackTrace()
		return exception
	}
}