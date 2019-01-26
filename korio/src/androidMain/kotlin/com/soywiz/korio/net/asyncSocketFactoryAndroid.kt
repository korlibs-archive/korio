package com.soywiz.korio.net

import com.soywiz.korio.async.*
import com.soywiz.korio.concurrent.atomic.*
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.*
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

class JvmAsyncClient(private var socket: Socket? = null) : AsyncClient {
	private val readQueue = AsyncThread()
	private val writeQueue = AsyncThread()

	private var socketIs: InputStream? = null
	private var socketOs: OutputStream? = null

	//suspend override fun connect(host: String, port: Int): Unit = suspendCoroutineEL { c ->
	override suspend fun connect(host: String, port: Int) {
		withContext(Dispatchers.IO) {
			socket = Socket(host, port)
			socketIs = socket?.getInputStream()
			socketOs = socket?.getOutputStream()
		}
	}

	override val connected: Boolean get() = socket?.isConnected ?: false

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int = readQueue { _read(buffer, offset, len) }
	//suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = _read(buffer, offset, len)

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = writeQueue {
		_write(buffer, offset, len)
	}

	//suspend private fun _read(buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutineEL { c ->
	private suspend fun _read(buffer: ByteArray, offset: Int, len: Int): Int = withContext(Dispatchers.IO) {
		socketIs?.read(buffer, offset, len) ?: 0
	}

	private suspend fun _write(buffer: ByteArray, offset: Int, len: Int): Unit {
		withContext(Dispatchers.IO) {
			socketOs?.write(buffer, offset, len)
		}
	}

	override suspend fun close() {
		withContext(Dispatchers.IO) {
			socket?.close()
			socketIs?.close()
			socketOs?.close()
			socket = null
			socketIs = null
			socketOs = null
		}
	}
}

class JvmAsyncServer(override val requestPort: Int, override val host: String, override val backlog: Int = -1) :
	AsyncServer {
	val ssc = ServerSocket()

	suspend fun init(): Unit {
		withContext(Dispatchers.IO) {
			ssc.bind(InetSocketAddress(host, requestPort), backlog)
		}
	}

	override val port: Int get() = (ssc.localSocketAddress as? InetSocketAddress)?.port ?: -1

	override suspend fun accept(): AsyncClient {
		return withContext(Dispatchers.IO) {
			JvmAsyncClient(ssc.accept())
		}
	}
}
