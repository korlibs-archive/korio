package com.soywiz.korio.util

@Deprecated("Use ?.let", replaceWith = ReplaceWith("this?.let(map)"))
inline fun <T, R> T?.nonNullMap(map: (T) -> R?): R? = if (this == null) null else map(this)