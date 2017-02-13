package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler

class JsJvmAsyncSocketFactory : AsyncSocketFactory {
	override suspend fun createClient(): AsyncClient = JsJvmAsyncClient()
	override suspend fun createServer(port: Int, host: String, backlog: Int): AsyncServer = JsJvmAsyncServer(port, host, backlog).apply { init() }
}

//private val newPool by lazy { Executors.newFixedThreadPool(1) }
//private val group by lazy { AsynchronousChannelGroup.withThreadPool(newPool) }
private val group by lazy { AsynchronousChannelGroup.withThreadPool(EventLoopExecutorService) }

class JsJvmAsyncClient(private var sc: AsynchronousSocketChannel? = null) : AsyncClient {
	private var _connected = false

	//suspend override fun connect(host: String, port: Int): Unit = suspendCoroutineEL { c ->
	suspend override fun connect(host: String, port: Int): Unit = korioSuspendCoroutine { c ->
		sc = AsynchronousSocketChannel.open(group)
		sc?.connect(InetSocketAddress(host, port), this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { _connected = true; c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { _connected = false; c.resumeWithException(exc) }
		})
	}

	override val connected: Boolean get() = sc?.isOpen ?: false

	//suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutineEL { c ->
	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = korioSuspendCoroutine { c ->
		val bb = ByteBuffer.wrap(buffer, offset, len)
		sc?.read(bb, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = c.resume(result)
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = c.resumeWithException(exc)
		}) ?: -1
	}

	//suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutineEL { c ->
	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = korioSuspendCoroutine { c ->
		val bb = ByteBuffer.wrap(buffer, offset, len)
		AsyncClient.Stats.writeCountStart.incrementAndGet()
		sc?.write(bb, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit {
				AsyncClient.Stats.writeCountEnd.incrementAndGet()
				c.resume(Unit)
			}

			override fun failed(exc: Throwable, attachment: AsyncClient): Unit {
				AsyncClient.Stats.writeCountError.incrementAndGet()
				c.resumeWithException(exc)
			}
		})
	}

	suspend override fun close(): Unit {
		sc?.close()
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

	suspend fun AsynchronousServerSocketChannel.saccept(): AsynchronousSocketChannel = korioSuspendCoroutine { c ->
		this.accept(kotlin.Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
			override fun completed(result: AsynchronousSocketChannel, attachment: Unit) = kotlin.Unit.apply { c.resume(result) }
			override fun failed(exc: Throwable, attachment: Unit) = kotlin.Unit.apply { c.resumeWithException(exc) }
		})
	}
}
