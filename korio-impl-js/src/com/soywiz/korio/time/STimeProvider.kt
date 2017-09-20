package com.soywiz.korio.time

import kotlin.js.Date

impl object STimeProvider {
	impl fun now(): Long = Date().getTime().toLong()
}