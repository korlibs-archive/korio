package com.soywiz.korio.net.ws

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.coroutines.*
import kotlin.random.*

object RawSocketWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(
		url: String,
		protocols: List<String>?,
		origin: String?,
		wskey: String?,
		debug: Boolean
	): WebSocketClient {
		val uri = URI(url)
		val secure = when (uri.scheme) {
			"ws" -> false
			"wss" -> true
			else -> error("Unknown ws protocol ${uri.scheme}")
		}
		val host = uri.host ?: "127.0.0.1"
		val port = uri.defaultPort.takeIf { it != URI.DEFAULT_PORT } ?: if (secure) 443 else 80
		val client = AsyncClient(host, port, secure = secure)

		return RawSocketWebSocketClient(
			coroutineContext,
			client,
			uri,
			protocols,
			debug,
			origin,
			wskey ?: "mykey"
		).apply {
			connect()
		}
	}
}

class WsFrame(val data: ByteArray, val type: WsOpcode, val isFinal: Boolean = true, val frameIsBinary: Boolean = true) {
	fun toByteArray(): ByteArray = MemorySyncStreamToByteArray {
		//Chrome: VM321:1 WebSocket connection to 'ws://localhost:8000/' failed: A server must not mask any frames that it sends to the client.
		val isMasked =
			false //true; // All clientes messages must be masked: http://tools.ietf.org/html/rfc6455#section-5.1
		val mask = Random.nextBytes(4)
		val sizeMask = (if (isMasked) 0x80 else 0x00)

		write8(type.id or (if (isFinal) 0x80 else 0x00))

		when {
			data.size < 126 -> write8(data.size or sizeMask)
			data.size < 65536 -> {
				write8(126 or sizeMask)
				write16_be(data.size)
			}
			else -> {
				write8(127 or sizeMask)
				write32_be(0)
				write32_be(data.size)
			}
		}

		if (isMasked) writeBytes(mask)

		writeBytes(if (isMasked) applyMask(data, mask) else data)
	}

	companion object {
		fun applyMask(payload: ByteArray, mask: ByteArray?): ByteArray {
			if (mask == null) return payload
			val maskedPayload = ByteArray(payload.size)
			for (n in 0 until payload.size) maskedPayload[n] = (payload[n].toInt() xor mask[n % mask.size].toInt()).toByte()
			return maskedPayload
		}
	}
}

class RawSocketWebSocketClient(
	val coroutineContext: CoroutineContext,
	val client: AsyncClient,
	url: URI,
	protocols: List<String>?,
	debug: Boolean,
	val origin: String?,
	val key: String
) : WebSocketClient(url.fullUri, protocols, debug) {
	private var frameIsBinary = false
	val host = url.host ?: "127.0.0.1"
	val port = url.port

	internal suspend fun connect() {
		client.writeBytes((buildList<String> {
			add("GET $url HTTP/1.1")
			add("Host: $host:$port")
			add("Pragma: no-cache")
			add("Cache-Control: no-cache")
			add("Upgrade: websocket")
			if (protocols != null) {
				add("Sec-WebSocket-Protocol: " + protocols.joinToString(", "))
			}
			add("Sec-WebSocket-Version: 13")
			add("Connection: Upgrade")
			add("Sec-WebSocket-Key: " + Base64.encode(key.toByteArray()))
			add("Origin: $origin")
			add("User-Agent: Mozilla/5.0")
		}.joinToString("\r\n") + "\r\n\n").toByteArray())

		// Read response
		val headers = arrayListOf<String>()
		while (true) {
			val line = client.readLine().trimEnd()
			if (line.isEmpty()) {
				headers += line
				break
			}
		}

		launchImmediately {
			onOpen(Unit)
			try {
				loop@ while (!closed) {
					val frame = readWsFrame()
					val payload = if (frame.frameIsBinary) frame.data else frame.data.toString(UTF8)
					when (frame.type) {
						WsOpcode.Close -> {
							break@loop
						}
						WsOpcode.Ping -> {
							sendWsFrame(WsFrame(frame.data, WsOpcode.Pong))
						}
						WsOpcode.Pong -> {
							lastPong = DateTime.now()
						}
						else -> {
							when (payload) {
								is String -> onStringMessage(payload)
								is ByteArray -> onBinaryMessage(payload)
							}
							onAnyMessage(payload)
						}
					}
				}
			} catch (e: Throwable) {
				onError(e)
			}
			onClose(Unit)
		}
	}

	private var lastPong: DateTime? = null

	var closed = false

	override fun close(code: Int, reason: String) {
		closed = true
		launchImmediately(coroutineContext) {
			sendWsFrame(WsFrame(byteArrayOf(), WsOpcode.Close))
		}
	}

	override suspend fun send(message: String) {
		sendWsFrame(WsFrame(message.toByteArray(UTF8), WsOpcode.Text))
	}

	override suspend fun send(message: ByteArray) {
		sendWsFrame(WsFrame(message, WsOpcode.Binary))
	}

	suspend fun readWsFrame(): WsFrame {
		val b0 = client.readU8()
		val b1 = client.readU8()

		val isFinal = b0.extract(7)
		val opcode = WsOpcode(b0.extract(0, 4))
		val frameIsBinary = when (opcode) {
			WsOpcode.Text -> false
			WsOpcode.Binary -> true
			else -> frameIsBinary
		}

		val partialLength = b1.extract(0, 7)
		val isMasked = b1.extract(7)

		val length = when (partialLength) {
			126 -> client.readU16_be()
			127 -> {
				val tmp = client.readS32_be()
				if(tmp != 0) error("message too long")
				client.readS32_be()
			}
			else -> partialLength
		}
		val mask = if (isMasked) client.readBytesExact(4) else null
		val unmaskedData = client.readBytesExact(length)
		val finalData = WsFrame.applyMask(unmaskedData, mask)
		return WsFrame(finalData, opcode, isFinal, frameIsBinary)
	}

	suspend fun sendWsFrame(frame: WsFrame) {
		client.writeBytes(frame.toByteArray())
	}
}

inline class WsOpcode(val id: Int) {
	companion object {
		val Continuation = WsOpcode(0x00)
		val Text = WsOpcode(0x01)
		val Binary = WsOpcode(0x02)
		val Close = WsOpcode(0x08)
		val Ping = WsOpcode(0x09)
		val Pong = WsOpcode(0x0A)
	}
}
