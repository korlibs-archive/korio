package com.soywiz.korio.net.http

import com.soywiz.korio.async.Promise
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.Extra
import java.io.IOException


open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = defaultHttpFactory.createServer()
	}

	abstract class BaseRequest(
			val uri: String
	) : Extra by Extra.Mixin() {
		val absoluteURI: String by lazy { uri }
	}

	abstract class WsRequest(
			uri: String
	) : BaseRequest(uri) {
		abstract suspend fun reject()

		abstract suspend fun close()

		abstract suspend fun onStringMessage(handler: suspend (String) -> Unit)

		abstract suspend fun onBinaryMessage(handler: suspend (ByteArray) -> Unit)

		abstract suspend fun onClose(handler: suspend () -> Unit)

		abstract suspend fun send(msg: String)

		abstract suspend fun send(msg: ByteArray)
	}

	abstract class Request(
			val method: Http.Method,
			uri: String,
			val headers: Http.Headers
	) : BaseRequest(uri) {

		fun getHeader(key: String): String? = headers[key]

		private var headersSent = false
		private val resHeaders = ArrayList<Pair<String, String>>()

		private fun ensureHeadersNotSent() {
			if (headersSent) {
				println("Sent headers: $resHeaders")
				throw IOException("Headers already sent")
			}
		}

		fun putHeader(key: String, value: String) {
			ensureHeadersNotSent()
			resHeaders += key to value
		}

		abstract protected fun _handler(handler: (ByteArray) -> Unit)
		abstract protected fun _endHandler(handler: () -> Unit)
		abstract protected fun _setStatus(code: Int, message: String)
		abstract protected fun _sendHeaders(headers: Http.Headers)
		abstract protected fun _emit(data: ByteArray)
		abstract protected fun _end()

		fun handler(handler: (ByteArray) -> Unit) {
			_handler(handler)
		}

		fun endHandler(handler: () -> Unit) {
			_endHandler(handler)
		}

		fun setStatus(code: Int, message: String = HttpStatusMessage(code)) {
			ensureHeadersNotSent()
			_setStatus(code, message)
		}

		private fun flushHeaders() {
			if (!headersSent) {
				headersSent = true
				_sendHeaders(Http.Headers(resHeaders))
			}
		}

		fun emit(data: ByteArray) {
			//println("EMIT: ${data.size}")
			flushHeaders()
			_emit(data)
		}

		fun end() {
			//println("END")
			flushHeaders()
			_end()
		}

		fun end(data: ByteArray) {
			putHeader("Content-Length", "${data.size}")
			emit(data)
			end()
		}

		fun emit(data: String) = emit(data.toByteArray(Charsets.UTF_8))
		fun end(data: String) = end(data.toByteArray(Charsets.UTF_8))
	}

	suspend open protected fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
	}

	suspend open protected fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
	}

	suspend fun allHandler(handler: suspend (BaseRequest) -> Unit) = this.apply {
		websocketHandler { handler(it) }
		httpHandler { handler(it) }
	}

	suspend open protected fun listenInternal(port: Int, host: String = "127.0.0.1") {
		val deferred = Promise.Deferred<Unit>()
		deferred.onCancel {

		}
		deferred.promise.await()
	}

	open val actualPort: Int = 0

	suspend open protected fun closeInternal() {
	}

	suspend fun websocketHandler(handler: suspend (WsRequest) -> Unit): HttpServer {
		websocketHandlerInternal(handler)
		return this
	}

	suspend fun httpHandler(handler: suspend (Request) -> Unit): HttpServer {
		httpHandlerInternal(handler)
		return this
	}

	suspend fun listen(port: Int = 0, host: String = "127.0.0.1"): HttpServer {
		listenInternal(port, host)
		return this
	}

	suspend fun listen(port: Int = 0, host: String = "127.0.0.1", handler: suspend (Request) -> Unit): HttpServer {
		httpHandler(handler)
		listen(port, host)
		return this
	}

	suspend final override fun close() {
		closeInternal()
	}
}
