package com.soywiz.korio.time

impl typealias Date = java.util.Date

impl fun UTC(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int): Long {
	return Date.UTC(year, month, day, hours, minutes, seconds)
}

@Suppress("DEPRECATION")
impl fun dateToTimestamp(year: Int, month: Int, day: Int, hours: Int, minutes: Int, seconds: Int): Long {
	return java.util.Date.UTC(year, month, day, hours, minutes, seconds)
}

impl fun currentTimeMillis(): Long = System.currentTimeMillis()
