package com.soywiz.korio.util

fun IntRange.toLongRange() = this.start.toLong() .. this.endInclusive.toLong()