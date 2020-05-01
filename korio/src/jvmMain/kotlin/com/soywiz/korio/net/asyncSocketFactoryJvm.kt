package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.concurrent.atomic.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import kotlin.coroutines.*

internal actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient = JvmAsyncClient()
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
			JvmAsyncServer(port, host, backlog).apply { init() }
	}
}

//private val newPool by lazy { Executors.newFixedThreadPool(1) }
//private val group by lazy { AsynchronousChannelGroup.withThreadPool(newPool) }

class JvmAsyncClient(private var sc: AsynchronousSocketChannel? = null) : AsyncClient {
	private val readQueue = AsyncThread()
	private val writeQueue = AsyncThread()

	//suspend override fun connect(host: String, port: Int): Unit = suspendCoroutineEL { c ->
	override suspend fun connect(host: String, port: Int): Unit = suspendCancellableCoroutine { c ->
		sc?.close()
		sc = AsynchronousSocketChannel.open(
			AsynchronousChannelGroup.withThreadPool(EventLoopExecutorService(c.context))
		)
		sc?.connect(InetSocketAddress(host, port), this, object : CompletionHandler<Void, AsyncClient> {
			override fun completed(result: Void?, attachment: AsyncClient): Unit = run { c.resume(Unit) }
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = run { c.resumeWithException(exc) }
		})
	}

	override val connected: Boolean get() = sc?.isOpen ?: false

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue { _read(buffer, offset, len) }
	//suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = _read(buffer, offset, len)

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
		_write(buffer, offset, len)
	}

	//suspend private fun _read(buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutineEL { c ->
	private suspend fun _read(buffer: ByteArray, offset: Int, len: Int): Int = suspendCancellableCoroutine { c ->
		if (sc == null) throw IOException("Not connected")
		val bb = ByteBuffer.wrap(buffer, offset, len)
		sc!!.read(bb, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient): Unit = c.resume(result)
			override fun failed(exc: Throwable, attachment: AsyncClient): Unit = c.resumeWithException(exc)
		})
	}

	private suspend fun _write(buffer: ByteArray, offset: Int, len: Int): Unit {
		_writeBufferFull(ByteBuffer.wrap(buffer, offset, len))
	}

	private suspend fun _writeBufferFull(bb: ByteBuffer) {
		while (bb.hasRemaining()) {
			_writeBufferPartial(bb)
		}
	}


	private suspend fun _writeBufferPartial(bb: ByteBuffer): Int = suspendCancellableCoroutine { c ->
		if (sc == null) {
			throw IOException("Not connected")
		}
		AsyncClient.Stats.writeCountStart.incrementAndGet()
		sc!!.write(bb, this, object : CompletionHandler<Int, AsyncClient> {
			override fun completed(result: Int, attachment: AsyncClient) {
				AsyncClient.Stats.writeCountEnd.incrementAndGet()
				c.resume(result)
			}

			override fun failed(exc: Throwable, attachment: AsyncClient) {
				//println("write failed")
				AsyncClient.Stats.writeCountError.incrementAndGet()
				c.resumeWithException(exc)
			}
		})
	}

	override suspend fun close() {
		sc?.close()
		sc = null
	}
}

class JvmAsyncServer(override val requestPort: Int, override val host: String, override val backlog: Int = -1) :
	AsyncServer {
	val ssc = AsynchronousServerSocketChannel.open()

	suspend fun init(): Unit {
		ssc.bind(InetSocketAddress(host, requestPort), backlog)
		for (n in 0 until 100) {
			if (ssc.isOpen) break
			delay(50)
		}
	}

	override val port: Int get() = (ssc.localAddress as? InetSocketAddress)?.port ?: -1

	override suspend fun accept(): AsyncClient = suspendCoroutine { c ->
		val ctx = c.context

		ssc.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
			override fun completed(result: AsynchronousSocketChannel, attachment: Unit) {
				launchImmediately(ctx) {
					c.resume(JvmAsyncClient(result))
				}
			}

			override fun failed(exc: Throwable, attachment: Unit) = run {
				exc.printStackTrace()
				c.resumeWithException(exc)
			}
		})
	}

    override suspend fun close() {
        try {
            ssc.close()
        }catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
