package com.soywiz.korio.lang

actual object AsyncCapture {
	actual suspend fun asyncCaptureStdout(callback: suspend () -> Unit): String {
		val original = console.asDynamic().log
		try {
			var out = ""
			console.asDynamic().log = { str: String ->
				out += "$str\n"
			}
			callback()
			return out
		} finally {
			console.asDynamic().log = original
		}
	}
}
