package com.soywiz.korio.net

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.suspendCoroutine

class AsyncServer(val local: SocketAddress, val backlog: Int = 128) {
	constructor(port: Int, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

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