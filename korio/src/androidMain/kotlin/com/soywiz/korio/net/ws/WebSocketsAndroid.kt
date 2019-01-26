package com.soywiz.korio.net.ws

actual suspend fun WebSocketClient(
	url: String,
	protocols: List<String>?,
	origin: String?,
	wskey: String?,
	debug: Boolean
): WebSocketClient = RawSocketWebSocketClient(url, protocols, origin, wskey, debug)
