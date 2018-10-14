package com.soywiz.korio.net

import com.soywiz.korio.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.atomicfu.*
import kotlin.coroutines.*

val asyncSocketFactory: AsyncSocketFactory get() = KorioNative.asyncSocketFactory

abstract class AsyncSocketFactory {
	abstract suspend fun createClient(): AsyncClient
	abstract suspend fun createServer(port: Int, host: String = "127.0.0.1", backlog: Int = 511): AsyncServer
}

interface AsyncClient : AsyncInputStream, AsyncOutputStream, AsyncCloseable {
	suspend fun connect(host: String, port: Int)
	val connected: Boolean
	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int
	override suspend fun write(buffer: ByteArray, offset: Int, len: Int)
	override suspend fun close()
	//suspend open fun reconnect() = Unit

	object Stats {
		val writeCountStart = atomic(0L)
		val writeCountEnd = atomic(0L)
		val writeCountError = atomic(0L)

		override fun toString(): String = "AsyncClient.Stats($writeCountStart/$writeCountEnd/$writeCountError)"
	}

	companion object {
		suspend operator fun invoke(host: String, port: Int) = createAndConnect(host, port)
		suspend fun create(): AsyncClient = asyncSocketFactory.createClient()
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
		suspend operator fun invoke(port: Int, host: String = "127.0.0.1", backlog: Int = -1) =
			asyncSocketFactory.createServer(port, host, backlog)
	}

	suspend fun listen(handler: suspend (AsyncClient) -> Unit): Closeable

	suspend fun listen(): SuspendingSequence<AsyncClient> {
		val ctx = coroutineContext
		return asyncGenerate3 {
			launchImmediately(ctx) {
				listen {
					yield(it)
				}
			}
		}
	}
}
