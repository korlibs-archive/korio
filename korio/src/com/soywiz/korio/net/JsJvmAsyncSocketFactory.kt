package com.soywiz.korio.net

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.sleep
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit
import kotlin.coroutines.suspendCoroutine

class JsJvmAsyncSocketFactory : AsyncSocketFactory {
	override suspend fun createClient(): AsyncClient = JsJvmAsyncClient()
	override suspend fun createServer(port: Int, host: String, backlog: Int): AsyncServer = JsJvmAsyncServer(port, host, backlog).apply { init() }
}

class JsJvmAsyncClient(private val sc: AsynchronousSocketChannel = AsynchronousSocketChannel.open()) : AsyncClient {
	private var _connected = false

	suspend override fun connect(host: String, port: Int): Unit = suspendCoroutine { c ->
		sc.connect(InetSocketAddress(host, port), this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { _connected = true; c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { _connected = false; c.resumeWithException(exc) }
		})
	}

	override val connected: Boolean get() = sc.isOpen

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

	suspend override fun close(): Unit {
		sc.close()
	}
}

class JsJvmAsyncServer(override val requestPort: Int, override val host: String, override val backlog: Int = 128) : AsyncServer {
	val ssc = AsynchronousServerSocketChannel.open()

	suspend fun init() {
		ssc.bind(InetSocketAddress(host, requestPort), backlog)
		for (n in 0 until 100) {
			if (ssc.isOpen) break
			sleep(50)
		}
	}

	override val port: Int get() = (ssc.localAddress as? InetSocketAddress)?.port ?: -1

	suspend override fun listen(): AsyncSequence<AsyncClient> = asyncGenerate {
		while (true) yield(JsJvmAsyncClient(ssc.saccept()))
	}

	suspend fun AsynchronousServerSocketChannel.saccept(): AsynchronousSocketChannel = suspendCoroutine { c ->
		this.accept(kotlin.Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
			override fun completed(result: AsynchronousSocketChannel, attachment: Unit) = kotlin.Unit.apply { c.resume(result) }
			override fun failed(exc: Throwable, attachment: Unit) = kotlin.Unit.apply { c.resumeWithException(exc) }
		})
	}
}
