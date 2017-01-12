package com.soywiz.korio.util

fun Char.isLetterOrUnderscore(): Boolean = this.isLetter() || this == '_' || this == '$'
fun Char.isLetterDigitOrUnderscore(): Boolean = this.isLetterOrDigit() || this == '_' || this == '$'
fun Char.isLetterOrDigitOrDollar(): Boolean = this.isLetterOrDigit() || this == '$'

// @TODO: Make a proper table
// 0x20, 0x7e
//return this.isLetterDigitOrUnderscore() || this == '.' || this == '/' || this == '\'' || this == '"' || this == '(' || this == ')' || this == '[' || this == ']' || this == '+' || this == '-' || this == '*' || this == '/'
fun Char.isPrintable(): Boolean = when (this) {
	in '\u0020'..'\u007e', in '\u00a1'..'\u00ff' -> true
	else -> false
}