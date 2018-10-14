package com.soywiz.korio

// @TODO: Make actual/expect so it works on JVM
@PublishedApi
internal inline fun <T> synchronized2(@Suppress("UNUSED_PARAMETER") obj: Any, callback: () -> T): T = callback()
