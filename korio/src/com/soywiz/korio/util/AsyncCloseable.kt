package com.soywiz.korio.util

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.await

interface AsyncCloseable {
	suspend fun close(): Unit
}

inline suspend fun <T : AsyncCloseable> T.use(callback: suspend T.() -> Unit): Unit = asyncFun {
	try {
		callback.await(this@use)
	} finally {
		close()
	}
}