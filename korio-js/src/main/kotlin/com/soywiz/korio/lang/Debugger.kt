package com.soywiz.korio.lang

impl object Debugger {
	impl fun enterDebugger() {
		js("debugger;")
	}
}