package com.soywiz.korio.util

val LONG_ZERO_TO_MAX_RANGE = 0..Long.MAX_VALUE
fun IntRange.toLongRange() = this.start.toLong()..this.endInclusive.toLong()