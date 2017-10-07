package com.soywiz.korio.lang

expect object AsyncCapture {
	suspend fun asyncCaptureStdout(callback: suspend () -> Unit): String
}

suspend fun asyncCaptureStdout(callback: suspend () -> Unit): String = AsyncCapture.asyncCaptureStdout(callback)
