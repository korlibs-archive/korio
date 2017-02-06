package com.soywiz.korio.net.ws

import com.jtransc.js.jsDebugger
import com.soywiz.korio.async.spawn
import com.soywiz.korio.crypto.Base64
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import java.io.EOFException
import java.net.URI
import kotlin.experimental.xor

class JvmWebSocketClientFactory : WebSocketClientFactory {
	override suspend fun create(url: URI, protocols: List<String>?, origin: String?, wskey: String?, debug: Boolean): WebSocketClient {
		return JvmWebSocketClient(url, protocols, origin, wskey, DEBUG = debug).apply { init() }
	}
}

class JvmWebSocketClient(url: URI, protocols: List<String>?, val origin: String?, val wskey: String?, DEBUG: Boolean) : WebSocketClient(url, protocols, true) {
	val DEBUG = DEBUG
	//val DEBUG = true
	lateinit var socket: AsyncClient
	val defaultPort = when (url.scheme) {
		"ws" -> 80
		"wss" -> 443
		else -> throw IllegalArgumentException("Just supported ws:// and ws:// protocols but found '${url.scheme}'")
	}
	val port = url.portWithDefault(defaultPort)

	suspend fun init() {
		socket = AsyncClient(url.host, port)
		// Thread
		val getRequest = prepareClientHandshake(url.toString(), url.host, port, wskey ?: "wskey", origin ?: "http://127.0.0.1/")
		if (DEBUG) println(getRequest.toString(Charsets.UTF_8))
		socket.writeBytes(getRequest)
		socket.apply {
			while (true) {
				val line = socket.readLine().trim()
				if (DEBUG) println(line)
				if (line.isEmpty()) break
			}
		}
		spawn {
			try {
				while (true) readFrame(socket)
			} catch (e: EOFException) {
			}
		}
	}

	val chunks = arrayListOf<ByteArray>()

	fun ByteArray.applyMask(mask: ByteArray, offset: Int = 0, len: Int = mask.size - offset): ByteArray {
		for (n in 0 until len) {
			this[offset + n] = this[offset + n] xor mask[n % mask.size]
		}
		return this
	}

	suspend fun readFrame(socket: AsyncClient) {
		//while (true) {
		//	val c = socket.readU8()
		//	println("%02X".format(c))
		//}

		val head1 = socket.readU8()
		val fin = (head1 and 0x80) != 0
		val op = head1 and 0xF
		val opcode = Opcode.IDS[op]
				?: throw IllegalStateException("Invalid Opcode $op")
		val head2 = socket.readU8()
		val masked = (head2 and 0x80) != 0
		val plen = (head2 and 0x7f)
		val len = when (plen) {
			126 -> socket.readU16_be()
			127 -> socket.readS32_be()
			else -> plen
		}
		val maskBytes = if (masked) socket.readBytes(4) else byteArrayOf()
		val payload = socket.readBytes(len)
		if (masked) payload.applyMask(maskBytes)

		if (DEBUG) println("[WS-RECV] Frame:$opcode:$len")

		when (opcode) {
			Opcode.CONTINUATION, Opcode.TEXT, Opcode.BINARY -> {
				chunks += payload
				if (fin) {
					val apayload = chunks.join()
					when (opcode) {
						Opcode.TEXT -> {
							val str = apayload.toString(Charsets.UTF_8)
							onStringMessage(str)
							onAnyMessage(str)
						}
						Opcode.BINARY -> {
							onBinaryMessage(apayload)
							onAnyMessage(apayload)
						}
						else -> throw IllegalStateException()
					}
					chunks.clear()
				}
			}
			Opcode.PING -> {
				sendFrame(Opcode.PONG, byteArrayOf())
			}
			Opcode.PONG -> {
			}
			Opcode.CLOSE -> {
				throw EOFException()
			}
		}
	}

	@Suppress("UNUSED_CHANGED_VALUE")
	fun prepareFrame(opcode: Opcode, payload: ByteArray, mask: Int = 0): ByteArray {
		val masked = mask != 0
		val extraLenSize = if (payload.size < 126) 0 else if (payload.size < 0x1_0000) 2 else 4
		val extraMaskSize = if (masked) 4 else 0
		val out = ByteArray(2 + extraLenSize + extraMaskSize + payload.size)
		var opos = 0
		val maskBytes = ByteArray(4).apply { write32_le(0, mask) }
		val fin = true

		out[opos++] = (opcode.id or (if (fin) 0x80 else 0)).toByte()
		out[opos++] = ((if (masked) 0x80 else 0) or (when (extraLenSize) {
			0 -> payload.size
			2 -> 126
			4 -> 127
			else -> TODO()
		})).toByte()

		when (extraLenSize) {
			2 -> {
				out.write16_be(opos, payload.size)
				opos += 2
			}
			4 -> {
				out.write32_be(opos, payload.size)
				opos += 4
			}
		}

		if (masked) {
			out.writeBytes(opos, maskBytes)
			opos += 4
		}

		out.writeBytes(opos, payload)
		if (masked) out.applyMask(maskBytes, opos, payload.size)

		return out
	}

	suspend fun sendFrame(opcode: Opcode, payload: ByteArray, mask: Int = -1) {
		if (DEBUG) println("[WS-SEND] Frame:$opcode:${payload.size}")

		socket.writeBytes(prepareFrame(opcode, payload, mask))
	}

	override suspend fun send(message: String) {
		sendFrame(Opcode.TEXT, message.toByteArray(Charsets.UTF_8))
	}

	override suspend fun send(message: ByteArray) {
		sendFrame(Opcode.BINARY, message)
	}

	private fun prepareClientHandshake(url: String, host: String, port: Int, key: String, origin: String): ByteArray {
		val lines = arrayListOf<String>();
		lines += "GET $url HTTP/1.1"
		lines += "Host: $host:$port"
		lines += "Pragma: no-cache"
		lines += "Cache-Control: no-cache"
		lines += "Upgrade: websocket"
		if (this.protocols != null) {
			lines += "Sec-WebSocket-Protocol: " + this.protocols.joinToString(", ")
		}
		lines += "Sec-WebSocket-Version: 13"
		lines += "Connection: Upgrade"
		lines += "Sec-WebSocket-Key: " + Base64.encode(key.toByteArray(Charsets.UTF_8))
		lines += "Origin: $origin"
		lines += "User-Agent: Mozilla/5.0"

		return (lines.joinToString("\r\n") + "\r\n\r\n").toByteArray(Charsets.UTF_8)
	}
}

enum class Opcode(val id: Int) {
	CONTINUATION(0x00),
	TEXT(0x01),
	BINARY(0x02),
	CLOSE(0x08),
	PING(0x09),
	PONG(0x0A);

	companion object {
		val IDS = values().map { it.id to it }.toMap()
	}
}