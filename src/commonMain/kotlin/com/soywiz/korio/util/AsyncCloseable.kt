package com.soywiz.korio.util

import com.soywiz.korio.async.*

interface AsyncCloseable {
	suspend fun close()

	companion object {
		val DUMMY = object : AsyncCloseable {
			override suspend fun close() = Unit
		}
	}
}

suspend inline fun <T : AsyncCloseable, TR> T.use(callback: T.() -> TR): TR {
	try {
		return callback(this@use)
	} finally {
		close()
	}
}
