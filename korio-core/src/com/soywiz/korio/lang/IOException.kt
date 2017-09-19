package com.soywiz.korio.lang

open class IOException(msg: String) : Exception(msg)
open class EOFException(msg: String) : IOException(msg)
open class FileNotFoundException(msg: String) : IOException(msg)
