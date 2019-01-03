package com.soywiz.korio.net.ws

actual val websockets: WebSocketClientFactory get() = com.soywiz.korio.net.ws.RawSocketWebSocketClientFactory

