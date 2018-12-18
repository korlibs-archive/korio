@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import kotlin.math.*

fun Int.toStringUnsigned(radix: Int): String = this.toUInt().toString(radix)
fun Long.toStringUnsigned(radix: Int): String = this.toULong().toString(radix)

val Float.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"
val Double.niceStr: String get() = if (round(this) == this) "${this.toLong()}" else "$this"
