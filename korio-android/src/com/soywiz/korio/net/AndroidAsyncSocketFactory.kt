@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.net

import com.soywiz.korio.async.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class AndroidAsyncSocketFactory : AsyncSocketFactory {
	override suspend fun createClient(): AsyncClient = AndroidAsyncClient()
	override suspend fun createServer(port: Int, host: String, backlog: Int): AsyncServer = asyncFun { AndroidAsyncServer(port, host, backlog).apply { init() } }
}

class AndroidAsyncClient(private val s: Socket = Socket()) : AsyncClient {
	private var _connected = false

	suspend override fun connect(host: String, port: Int): Unit = executeInWorker {
		s.connect(InetSocketAddress(host, port))
	}

	override val connected: Boolean get() = s.isConnected

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
		s.inputStream.read(buffer, offset, len)
	}

	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = executeInWorker {
		s.outputStream.write(buffer, offset, len)
	}

	suspend override fun close(): Unit = executeInWorker {
		s.close()
	}
}

class AndroidAsyncServer(override val requestPort: Int, override val host: String, override val backlog: Int = 128) : AsyncServer {
	private val s: ServerSocket = ServerSocket()

	suspend fun init() = executeInWorker {
		s.bind(InetSocketAddress(host, requestPort))
		for (n in 0 until 100) {
			if (s.isBound) break
			sleep(50)
		}
	}

	override val port: Int get() = s.localPort

	suspend override fun listen(): AsyncSequence<AsyncClient> = asyncGenerate {
		while (true) yield(AndroidAsyncClient(executeInWorker { s.accept() }))
	}
}
