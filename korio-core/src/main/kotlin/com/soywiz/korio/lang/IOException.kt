package com.soywiz.korio.lang

header open class IOException(msg: String) : Exception(msg)
header open class EOFException(msg: String) : IOException(msg)
header open class FileNotFoundException(msg: String) : IOException(msg)
