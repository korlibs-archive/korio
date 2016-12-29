package com.soywiz.korio.net

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

class AsyncClient(
	private val sc: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) : AsyncStream() {
	private var _connected = false

	companion object {
		suspend operator fun invoke(host: String, port: Int) = createAndConnect(host, port)

		suspend fun createAndConnect(host: String, port: Int) = asyncFun {
			val socket = AsyncClient()
			socket.connect(host, port)
			socket
		}
	}

	suspend fun connect(host: String, port: Int) = connect(InetSocketAddress(host, port))

	suspend fun connect(remote: SocketAddress): Unit = suspendCoroutine { c ->
		sc.connect(remote, this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { _connected = true; c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { _connected = false; c.resumeWithException(exc) }
		})
	}

	val connected: Boolean get() = sc.isOpen

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutine { c ->
		val bb = ByteBuffer.wrap(buffer, offset, len)

		sc.read(bb, 10L, TimeUnit.SECONDS, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = c.resume(result)
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = c.resumeWithException(exc)
		})
	}

	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
		val bb = ByteBuffer.wrap(buffer, offset, len)
		sc.write(bb, 10L, TimeUnit.SECONDS, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = run { c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { c.resumeWithException(exc) }
		})
	}

	suspend override fun close(): Unit = asyncFun {
		sc.close()
	}
}
