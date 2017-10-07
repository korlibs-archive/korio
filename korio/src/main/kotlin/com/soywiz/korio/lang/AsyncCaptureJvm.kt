package com.soywiz.korio.lang

import java.io.ByteArrayOutputStream
import java.io.PrintStream

actual object AsyncCapture {
	actual suspend fun asyncCaptureStdout(callback: suspend () -> Unit): String {
		val ori = java.lang.System.out
		try {
			val baos = ByteArrayOutputStream()
			val ps = PrintStream(baos, true, "UTF-8")
			java.lang.System.setOut(ps)
			callback()
			return baos.toByteArray().toString(Charsets.UTF_8)
		} finally {
			java.lang.System.setOut(ori)
		}
	}
}
