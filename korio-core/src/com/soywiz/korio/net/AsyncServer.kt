package com.soywiz.korio.net

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.sleep
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.suspendCoroutine

class AsyncServer private constructor(val local: SocketAddress, val backlog: Int = 128) {
	private constructor(port: Int, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

	companion object {
		operator suspend fun invoke(port: Int, host: String = "127.0.0.1") = asyncFun {
			val server = AsyncServer(port, host)
			for (n in 0 until 100) {
				if (server.ssc.isOpen) break
				sleep(50)
			}
			server
		}
	}

	val ssc = AsynchronousServerSocketChannel.open()

	init {
		ssc.bind(local, backlog)
	}

	val port: Int get() = (ssc.localAddress as? InetSocketAddress)?.port ?: -1

	suspend fun listen(): AsyncSequence<AsyncClient> = asyncGenerate {
		while (true) yield(AsyncClient(ssc.saccept()))
	}

	suspend fun AsynchronousServerSocketChannel.saccept(): AsynchronousSocketChannel = suspendCoroutine { c ->
		this.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
			override fun completed(result: AsynchronousSocketChannel, attachment: Unit) = Unit.apply { c.resume(result) }
			override fun failed(exc: Throwable, attachment: Unit) = Unit.apply { c.resumeWithException(exc) }
		})
	}
}