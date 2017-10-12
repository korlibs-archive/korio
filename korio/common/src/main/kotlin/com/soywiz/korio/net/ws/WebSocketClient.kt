package com.soywiz.korio.net.ws

import com.soywiz.korio.KorioNative
import com.soywiz.korio.async.Signal
import com.soywiz.korio.lang.IOException

abstract class WebSocketClient protected constructor(val url: String, val protocols: List<String>?, int: Boolean) {
	val onOpen = Signal<Unit>()
	val onError = Signal<Throwable>()
	val onClose = Signal<Unit>()

	val onBinaryMessage = Signal<ByteArray>()
	val onStringMessage = Signal<String>()
	val onAnyMessage = Signal<Any>()

	open fun close(code: Int = 0, reason: String = ""): Unit = Unit
	suspend open fun send(message: String): Unit = Unit
	suspend open fun send(message: ByteArray): Unit = Unit
}

suspend fun WebSocketClient(url: String, protocols: List<String>? = null, origin: String? = null, wskey: String? = "wskey", debug: Boolean = false) = websockets.create(url, protocols, origin = origin, wskey = wskey, debug = debug)

val websockets: WebSocketClientFactory get() = KorioNative.websockets

abstract class WebSocketClientFactory {
	suspend abstract fun create(url: String, protocols: List<String>? = null, origin: String? = null, wskey: String? = null, debug: Boolean = false): WebSocketClient
}

class WebSocketException(message: String) : IOException(message)