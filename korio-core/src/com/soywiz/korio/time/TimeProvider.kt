package com.soywiz.korio.time

header object STimeProvider {
	fun now(): Long
}

object TimeProvider {
	fun now(): Long = STimeProvider.now()
}
