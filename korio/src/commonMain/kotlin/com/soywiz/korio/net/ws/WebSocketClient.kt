package com.soywiz.korio.net.ws

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.http.Http

abstract class WebSocketClient protected constructor(val url: String, val protocols: List<String>?, debug: Boolean) {
	val onOpen = Signal<Unit>()
	val onError = Signal<Throwable>()
	val onClose = Signal<Unit>()

	val onBinaryMessage = Signal<ByteArray>()
	val onStringMessage = Signal<String>()
	val onAnyMessage = Signal<Any>()

	open fun close(code: Int = 0, reason: String = ""): Unit = Unit
	open suspend fun send(message: String): Unit = Unit
	open suspend fun send(message: ByteArray): Unit = Unit
}

suspend fun WebSocketClient.readString() = onStringMessage.waitOneBase()
suspend fun WebSocketClient.readBinary() = onBinaryMessage.waitOneBase()

expect suspend fun WebSocketClient(
	url: String,
	protocols: List<String>? = null,
	origin: String? = null,
	wskey: String? = "wskey",
	debug: Boolean = false,
    headers: Http.Headers = Http.Headers()
): WebSocketClient

class WebSocketException(message: String) : IOException(message)
