package com.soywiz.korio.util

import kotlin.reflect.*

inline fun <reified T : Throwable> expectException(callback: () -> Unit) {
	var thrown: Throwable? = null
	try {
		callback()
	} catch (e: Throwable) {
		thrown = e
	}
	if (thrown == null || thrown !is T) {
		throw ExpectedException(T::class, thrown)
	}
}

class ExpectedException(val expectedClass: KClass<*>, val found: Throwable?)
	: Exception(if (found != null) "Expected $expectedClass but found $found" else "Expected $expectedClass no exception was thrown")
