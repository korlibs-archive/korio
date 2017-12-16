package com.soywiz.korio.util

@Deprecated("Use ?.let", replaceWith = ReplaceWith("this?.let(map)"))
inline fun <T, R> T?.nonNullMap(map: (T) -> R?): R? = if (this == null) null else map(this)

inline fun <T> T.nullIf(cond: T.() -> Boolean): T? = if (cond(this)) null else this