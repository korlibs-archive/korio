package com.soywiz.korio.net.ws

import com.soywiz.korio.*

actual val websockets: WebSocketClientFactory by lazy { JsWebSocketClientFactory() }
