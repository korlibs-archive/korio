package com.soywiz.korio.error

import java.lang.Exception

class InvalidOperationException(str: String = "Invalid Operation", cause: Throwable? = null) : Exception(str, cause)
class OutOfBoundsException(index: Int = -1, str: String = "Out Of Bounds") : Exception(str)
class KeyNotFoundException(str: String = "Key Not Found") : Exception(str)
class NotImplementedException(str: String = "Not Implemented") : Exception(str)
class InvalidArgumentException(str: String = "Invalid Argument") : Exception(str)
class MustValidateCodeException(str: String = "Must Validate Code") : Exception(str)
class MustOverrideException(str: String = "Must Override") : Exception(str)
class DeprecatedException(str: String = "Deprecated") : Exception(str)
class UnexpectedException(str: String = "Unexpected") : Exception(str)

val deprecated: Nothing get() = throw MustValidateCodeException()
val mustValidate: Nothing get() = throw NotImplementedException()
val noImpl: Nothing get() = throw NotImplementedException()
val invalidOp: Nothing get() = throw InvalidOperationException()

fun deprecated(msg: String): Nothing = throw DeprecatedException(msg)
fun mustValidate(msg: String): Nothing = throw MustValidateCodeException(msg)
fun noImpl(msg: String): Nothing = throw NotImplementedException(msg)
fun invalidOp(msg: String, cause: Throwable? = null): Nothing = throw InvalidOperationException(msg, cause)
fun unsupported(msg: String): Nothing = throw java.lang.UnsupportedOperationException(msg)
fun invalidArgument(msg: String): Nothing = throw InvalidArgumentException(msg)
fun unexpected(msg: String): Nothing = throw UnexpectedException(msg)

// Warns
fun untestedWarn(msg: String): Unit = println("Untested: $msg")

fun noImplWarn(msg: String): Unit = println("Not implemented: $msg")

inline fun <T> ignoreErrors(show: Boolean = false, action: () -> T): T? {
	try {
		return action()
	} catch (e: Throwable) {
		if (show) e.printStackTrace()
		return null
	}
}