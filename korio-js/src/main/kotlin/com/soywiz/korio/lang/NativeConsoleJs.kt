package com.soywiz.korio.lang

actual object NativeConsole {
	actual fun log(msg: Any?): Unit {
		console.log(msg)
	}

	actual fun error(msg: Any?): Unit {
		console.error(msg)
	}
}