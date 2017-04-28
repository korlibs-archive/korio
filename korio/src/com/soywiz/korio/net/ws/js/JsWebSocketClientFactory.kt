package com.soywiz.korio.net.ws.js

import com.jtransc.annotation.JTranscMethodBody
import com.jtransc.js.*
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.WebSocketClientFactory
import java.net.URI

class JsWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(url: URI, protocols: List<String>?, origin: String?, wskey: String?, debug: Boolean): WebSocketClient = JsWebSocketClient(url, protocols, DEBUG = debug).apply { init() }
}

class JsWebSocketClient(url: URI, protocols: List<String>?, val DEBUG: Boolean) : WebSocketClient(url, protocols, true) {
	val jsws = if (protocols != null) {
		jsNew("WebSocket", url, jsArray(*protocols.toTypedArray()))
	} else {
		jsNew("WebSocket", url)
	}.apply {
		this["binaryType"] = "arraybuffer"
		this.call("addEventListener", "open", jsFunctionRaw1 { onOpen(Unit) })
		this.call("addEventListener", "close", jsFunctionRaw1 { event ->
			var code = event["code"].toInt()
			var reason = event["reason"].toJavaStringOrNull()
			var wasClean = event["wasClean"].toBool()
			onClose(Unit)
		})
		this.call("addEventListener", "message", jsFunctionRaw1 { event ->
			val data = event["data"]
			if (DEBUG) println("[WS-RECV]: ${data.toJavaStringOrNull()} :: stringListeners=${onStringMessage.listenerCount}, binaryListeners=${onBinaryMessage.listenerCount}, anyListeners=${onAnyMessage.listenerCount}")
			if (data.jsIsString()) {
				val js = data.toJavaString()
				onStringMessage(js)
				onAnyMessage(js)
			} else {
				val jb = data.toByteArray()
				//console.methods["log"](event)
				//console.methods["log"](data)
				//console.methods["log"](data.toByteArray())
				onBinaryMessage(jb)
				onAnyMessage(jb)
			}
		})
	}

	suspend fun init() {
		if (DEBUG) println("[WS] Wait connection ($url)...")
		onOpen.waitOne()
		if (DEBUG) println("[WS] Connected!")
	}

	override fun close(code: Int, reason: String) {
		//jsws.methods["close"](code, reason)
		jsws.call("close")
	}

	override suspend fun send(message: String) {
		if (DEBUG) println("[WS-SEND]: $message")
		jsws.call("send", message)
	}

	override suspend fun send(message: ByteArray) {
		if (DEBUG) println("[WS-SEND]: ${message.toList()}")
		jsws.call("send", message.toJsTypedArray())
	}
}

@JTranscMethodBody(target = "js", value = "return this instanceof p0;")
external fun JsDynamic?.jsInstanceOf(type: JsDynamic?): JsDynamic?

fun JsDynamic?.jsIsString(): Boolean = this.typeOf().toJavaString() == "string"

@JTranscMethodBody(target = "js", value = "return JA_B.fromTypedArray(new Int8Array(p0, 0, p0.byteLength));")
external fun JsDynamic?.toByteArray(): ByteArray

@JTranscMethodBody(target = "js", value = "return new Int8Array(p0.data.buffer, 0, p0.length);")
external fun ByteArray.toJsTypedArray(): JsDynamic?
