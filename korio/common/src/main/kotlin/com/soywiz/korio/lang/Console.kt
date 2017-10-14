package com.soywiz.korio.lang

import com.soywiz.korio.KorioNative

object Console {
	fun error(msg: Any?) = KorioNative.error(msg)
	fun log(msg: Any?) = KorioNative.log(msg)
	fun err_print(str: String) = print(str)
	fun out_print(str: String) = print(str)
}