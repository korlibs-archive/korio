package com.soywiz.korio.util

val IntRange.length: Int get() = (this.endInclusive - this.start) + 1
val LongRange.length: Long get() = (this.endInclusive - this.start) + 1

val IntRange.endExclusive: Int get() = this.endInclusive + 1
val LongRange.endExclusive: Long get() = this.endInclusive + 1
