package com.soywiz.korio.net

import com.soywiz.korio.KorioNative
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.lang.AtomicLong
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncOutputStream
import com.soywiz.korio.util.AsyncCloseable

abstract class AsyncSocketFactory {
	suspend abstract fun createClient(): AsyncClient
	suspend abstract fun createServer(port: Int, host: String = "127.0.0.1", backlog: Int = 128): AsyncServer
}

val asyncSocketFactory: AsyncSocketFactory get() = KorioNative.asyncSocketFactory

interface AsyncClient : AsyncInputStream, AsyncOutputStream, AsyncCloseable {
	suspend fun connect(host: String, port: Int): Unit
	val connected: Boolean
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit
	suspend override fun close(): Unit

	//suspend open fun reconnect() = Unit

	object Stats {
		val writeCountStart = AtomicLong()
		val writeCountEnd = AtomicLong()
		val writeCountError = AtomicLong()

		override fun toString(): String = "AsyncClient.Stats($writeCountStart/$writeCountEnd/$writeCountError)"
	}

	companion object {
		suspend operator fun invoke(host: String, port: Int) = createAndConnect(host, port)

		suspend fun create(): AsyncClient {
			return asyncSocketFactory.createClient()
		}

		suspend fun createAndConnect(host: String, port: Int): AsyncClient {
			val socket = asyncSocketFactory.createClient()
			socket.connect(host, port)
			return socket
		}
	}
}

interface AsyncServer {
	val requestPort: Int
	val host: String
	val backlog: Int
	val port: Int

	companion object {
		operator suspend fun invoke(port: Int, host: String = "127.0.0.1", backlog: Int = -1) = asyncSocketFactory.createServer(port, host, backlog)
	}

	suspend fun listen(): AsyncSequence<AsyncClient>
}
