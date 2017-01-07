package com.soywiz.korio.net.ws

import java.util.*

val websockets: WebSocketClientFactory by lazy {
	ServiceLoader.load(WebSocketClientFactory::class.java).firstOrNull()
		?: throw UnsupportedOperationException("WebSocketClientFactory implementation not found!")
}
