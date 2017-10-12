package com.soywiz.korio.lang

import com.soywiz.korio.KorioNative

object Debugger {
	fun enterDebugger() = KorioNative.enterDebugger()
}