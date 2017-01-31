package com.soywiz.korio.util

inline fun <reified T : Throwable> expectException(callback: () -> Unit) = expectException(T::class.java, callback)

inline fun <T : Throwable> expectException(ex: Class<T>, callback: () -> Unit) {
	try {
		callback()
		throw ExpectedException("Expected $ex")
	} catch (e: Throwable) {
		if (!e.javaClass.isAssignableFrom(ex)) {
			throw e
		}
	}
}

class ExpectedException(msg: String) : Exception(msg)