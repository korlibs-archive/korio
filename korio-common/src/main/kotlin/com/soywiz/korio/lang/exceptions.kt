package com.soywiz.korio.lang

header open class IOException(msg: String) : Exception(msg)
header open class EOFException(msg: String) : IOException(msg)
header open class FileNotFoundException(msg: String) : IOException(msg)

header open class RuntimeException(msg: String) : Exception(msg)
header open class IllegalStateException(msg: String) : RuntimeException(msg)
header open class CancellationException(msg: String) : IllegalStateException(msg)

//fun IOException() = IOException("")
//fun EOFException() = EOFException("")
//fun FileNotFoundException() = FileNotFoundException("")
//fun RuntimeException() = RuntimeException("")
//fun IllegalStateException() = IllegalStateException("")
//fun CancellationException() = CancellationException("")