package com.soywiz.korio.time

impl object STimeProvider {
	impl fun now(): Long = System.currentTimeMillis()
}