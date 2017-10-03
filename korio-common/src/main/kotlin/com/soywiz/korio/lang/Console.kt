package com.soywiz.korio.lang

expect object NativeConsole {
	fun log(msg: Any?): Unit
	fun error(msg: Any?): Unit
}

object Console {
	fun error(msg: Any?) {
		NativeConsole.error(msg)
		// @TODO:
		//println("Error: $msg")
	}

	fun log(msg: Any?) {
		// @TODO:
		NativeConsole.log(msg)
		//println(msg)
	}

	fun err_print(str: String) {
		print(str)
	}

	fun out_print(str: String) {
		print(str)
	}
}