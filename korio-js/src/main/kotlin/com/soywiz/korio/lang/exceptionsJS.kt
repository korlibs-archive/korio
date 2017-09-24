package com.soywiz.korio.lang

impl open class IOException impl constructor(msg: String) : Exception(msg)
impl open class EOFException impl constructor(msg: String) : IOException(msg)
impl open class FileNotFoundException impl constructor(msg: String) : IOException(msg)

impl open class RuntimeException impl constructor(msg: String) : Exception(msg)
impl open class IllegalStateException impl constructor(msg: String) : RuntimeException(msg)
impl open class CancellationException impl constructor(msg: String) : IllegalStateException(msg)