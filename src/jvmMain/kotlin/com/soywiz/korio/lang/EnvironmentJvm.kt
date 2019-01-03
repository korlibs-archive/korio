package com.soywiz.korio.lang

actual object Environment {
	// Uses querystring on JS/Browser, and proper env vars in the rest
	actual operator fun get(key: String): String? {
		return System.getenv(key)
	}
}
