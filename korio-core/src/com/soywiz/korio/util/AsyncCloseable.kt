package com.soywiz.korio.util

import com.soywiz.korio.async.await

interface AsyncCloseable {
	suspend fun close(): Unit
}

inline suspend fun <T : AsyncCloseable, TR> T.use(callback: suspend T.() -> TR): TR {
	try {
		return callback.await(this@use)
	} finally {
		close()
	}
}