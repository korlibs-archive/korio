package com.soywiz.korio.net.ws

import com.soywiz.korio.service.Services

val websockets: WebSocketClientFactory by lazy {
	Services.load(WebSocketClientFactory::class.java)
}
