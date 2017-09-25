package com.soywiz.korio.util

import kotlin.browser.window

impl object SOS {
	impl val name: String by lazy {
		if (jsTypeOf(window) === undefined) {
			"node.js"
		} else {
			"js"
		}
	}
}