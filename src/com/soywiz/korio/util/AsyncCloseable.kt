package com.soywiz.korio.util

interface AsyncCloseable {
	suspend fun close(): Unit
}
