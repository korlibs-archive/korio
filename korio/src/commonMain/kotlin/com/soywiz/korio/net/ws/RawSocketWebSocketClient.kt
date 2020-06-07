package com.soywiz.korio.net.ws

import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.korio.util.encoding.*
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.random.*

suspend fun RawSocketWebSocketClient(
    url: String,
    protocols: List<String>? = null,
    origin: String? = null,
    wskey: String? = "wskey",
    debug: Boolean = false,
    connect: Boolean = true
): WebSocketClient {
    if (OS.isJsBrowserOrWorker) error("RawSocketWebSocketClient is not supported on JS browser. Use WebSocketClient instead")
    val uri = URL(url)
    val secure: Boolean = uri.isSecureScheme
    return RawSocketWebSocketClient(coroutineContext, AsyncClient.create(secure = secure), uri, protocols, debug, origin, wskey ?: "mykey").also { if (connect) it.connect() }
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
                write16BE(data.size)
            }
            else -> {
                write8(127 or sizeMask)
                write32BE(0)
                write32BE(data.size)
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
    val urlUrl: URL,
    protocols: List<String>?,
    debug: Boolean,
    val origin: String?,
    val key: String
) : WebSocketClient(urlUrl.fullUrl, protocols, debug) {
    private var frameIsBinary = false
    val host = urlUrl.host ?: "127.0.0.1"
    val port = urlUrl.port

    internal fun buildHeader(): String {
        return (buildList<String> {
            add("GET ${urlUrl.pathWithQuery} HTTP/1.1")
            add("Host: $host:$port")
            add("Pragma: no-cache")
            add("Cache-Control: no-cache")
            add("Upgrade: websocket")
            if (protocols != null) {
                add("Sec-WebSocket-Protocol: ${protocols.joinToString(", ")}")
            }
            add("Sec-WebSocket-Version: 13")
            add("Connection: Upgrade")
            add("Sec-WebSocket-Key: ${key.toByteArray().toBase64()}")
            if (origin != null) {
                add("Origin: $origin")
            }
            add("User-Agent: ${HttpClient.DEFAULT_USER_AGENT}")
        }.joinToString("\r\n") + "\r\n\r\n")
    }

    internal suspend fun connect() {
        if (OS.isJsBrowserOrWorker) error("RawSocketWebSocketClient is not supported on JS browser. Use WebSocketClient instead")

        client.connect(host, port)
        client.writeBytes(buildHeader().toByteArray())

        // Read response
        val headers = arrayListOf<String>()
        while (true) {
            val line = client.readLine().trimEnd()
            if (line.isEmpty()) {
                headers += line
                break
            }
        }

        launchImmediately(coroutineContext) {
            onOpen(Unit)
            try {
                loop@ while (!closed) {
                    val frame = readWsFrame()
                    val payload: Any = if (frame.frameIsBinary) frame.data else frame.data.toString(UTF8)
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
            126 -> client.readU16BE()
            127 -> {
                val tmp = client.readS32BE()
                if (tmp != 0) error("message too long")
                client.readS32BE()
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
