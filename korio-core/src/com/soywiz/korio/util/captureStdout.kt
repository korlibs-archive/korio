package com.soywiz.korio.util

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream

object StdoutRouterStream : OutputStream() {
	val defaultRoute = System.out

	init {
		System.setOut(StdoutRouter)
	}

	private val routePerThread = ThreadLocal<OutputStream>()

	override fun write(b: Int) {
		val os = routePerThread.get()
		if (os != null) {
			os.write(b)
		} else {
			defaultRoute.write(b)
		}
	}

	override fun write(b: ByteArray?, off: Int, len: Int) {
		val os = routePerThread.get()
		if (os != null) {
			os.write(b, off, len)
		} else {
			defaultRoute.write(b, off, len)
		}
	}

	fun <TOutputStream : OutputStream> routeTemporally(out: TOutputStream, callback: () -> Unit): TOutputStream {
		routePerThread.set(out)
		try {
			callback()
		} finally {
			routePerThread.set(null)
		}
		return out
	}

	fun captureThreadSafe(callback: () -> Unit) = routeTemporally(ByteArrayOutputStream(), callback)
}

object StdoutRouter : PrintStream(StdoutRouterStream) {
	val os = this.out
}

fun captureStdout(callback: () -> Unit): String {
	return StdoutRouterStream.captureThreadSafe(callback).toString()
}