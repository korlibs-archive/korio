package com.soywiz.korio.net

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncOutputStream
import com.soywiz.korio.util.AsyncCloseable

interface AsyncSocketFactory {
	suspend fun createClient(): AsyncClient
	suspend fun createServer(port: Int, host: String = "127.0.0.1", backlog: Int = 128): AsyncServer
}

interface AsyncClient : AsyncInputStream, AsyncOutputStream, AsyncCloseable {
	suspend fun connect(host: String, port: Int): Unit
	val connected: Boolean
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit
	suspend override fun close(): Unit

	companion object {
		suspend operator fun invoke(host: String, port: Int) = createAndConnect(host, port)

		suspend fun createAndConnect(host: String, port: Int) = asyncFun {
			val socket = asyncSocketFactory.createClient()
			socket.connect(host, port)
			socket
		}
	}
}

interface AsyncServer {
	val requestPort: Int
	val host: String
	val backlog: Int
	val port: Int

	companion object {
		operator suspend fun invoke(port: Int, host: String = "127.0.0.1", backlog: Int = 128) = asyncFun {
			asyncSocketFactory.createServer(port, host, backlog)
		}
	}

	suspend fun listen(): AsyncSequence<AsyncClient>
}
