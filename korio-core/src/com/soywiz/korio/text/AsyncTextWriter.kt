package com.soywiz.korio.text

interface AsyncTextWriter {
	suspend fun write(text: String)
}

interface AsyncTextWriterContainer {
	suspend fun write(writer: suspend (String) -> Unit)
}