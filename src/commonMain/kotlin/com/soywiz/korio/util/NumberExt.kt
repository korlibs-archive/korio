@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korio.util

import com.soywiz.kmem.*
import kotlin.math.*

private val DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

fun Long.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0L) return "0"
	var out = ""
	while (temp != 0L) {
		val digit = temp % radix
		temp /= radix
		out += DIGITS[digit.toInt()]
	}
	val rout = out.reversed()
	return if (isNegative) "-$rout" else rout
}

fun Int.toString(radix: Int): String {
	val isNegative = this < 0
	var temp = abs(this)
	if (temp == 0) return "0"
	var out = ""
	while (temp != 0) {
		val digit = temp % radix
		temp /= radix
		out += DIGITS[digit.toInt()]
	}
	val rout = out.reversed()
	return if (isNegative) "-$rout" else rout
}

fun Int.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0) return "0"
	var out = ""
	while (temp != 0) {
		val digit = temp urem radix
		temp = temp udiv radix
		out += DIGITS[digit]
	}
	return out.reversed()
}

fun Long.toStringUnsigned(radix: Int): String {
	var temp = this
	if (temp == 0L) return "0"
	var out = ""
	while (temp != 0L) {
		val digit = temp urem radix.toLong()
		temp = temp udiv radix.toLong()
		out += DIGITS[digit.toInt()]
	}
	return out.reversed()
}
