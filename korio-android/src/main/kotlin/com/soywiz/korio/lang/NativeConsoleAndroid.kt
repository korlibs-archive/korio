package com.soywiz.korio.lang

actual object NativeConsole {
	actual fun log(msg: Any?): Unit {
		java.lang.System.out.println(msg)
	}

	actual fun error(msg: Any?): Unit {
		java.lang.System.err.println(msg)
	}
}