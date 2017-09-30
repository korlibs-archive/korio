package com.soywiz.korio.util

import kotlin.browser.window

actual object SOS {
	actual val name: String by lazy {
		if (jsTypeOf(window) === undefined) {
			"node.js"
		} else {
			"js"
		}
	}
}