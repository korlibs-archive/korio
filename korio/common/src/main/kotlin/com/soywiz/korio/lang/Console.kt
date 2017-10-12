package com.soywiz.korio.lang

import com.soywiz.korio.KorioNative

object Console {
	fun error(msg: Any?) {
		KorioNative.error(msg)
		// @TODO:
		//println("Error: $msg")
	}

	fun log(msg: Any?) {
		// @TODO:
		KorioNative.log(msg)
		//println(msg)
	}

	fun err_print(str: String) {
		print(str)
	}

	fun out_print(str: String) {
		print(str)
	}
}