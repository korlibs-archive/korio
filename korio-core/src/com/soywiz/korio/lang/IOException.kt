package com.soywiz.korio.lang

open class IOException(msg: String) : Exception(msg)
open class EOFException(msg: String) : IOException(msg)