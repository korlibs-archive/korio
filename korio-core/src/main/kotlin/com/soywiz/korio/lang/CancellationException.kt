package com.soywiz.korio.lang

header open class RuntimeException : Exception()
header open class IllegalStateException : RuntimeException()
header open class CancellationException : IllegalStateException()