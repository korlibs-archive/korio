package com.soywiz.korio.net.http

import com.soywiz.korio.async.Promise
import com.soywiz.korio.util.AsyncCloseable
import java.io.IOException


open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = defaultHttpFactory.createServer()
	}

	abstract class Request(
		val method: Http.Method,
		val uri: String,
		val headers: Http.Headers
	) {
		val absoluteURI: String by lazy { uri }

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

		fun pathParam(name: String): String {
			println("WARNING: Not implemented: Request.pathParam!!")
			return ""
		}
	}

	suspend open protected fun listenInternal(port: Int, host: String = "127.0.0.1", handler: suspend (Request) -> Unit) {
		val deferred = Promise.Deferred<Unit>()
		deferred.onCancel {

		}
		deferred.promise.await()
	}

	open val actualPort: Int = 0

	suspend open protected fun closeInternal() {
	}

	suspend fun listen(port: Int, host: String = "127.0.0.1", handler: suspend (Request) -> Unit): HttpServer {
		listenInternal(port, host, handler)
		return this
	}

	suspend override fun close() {
		closeInternal()
	}
}
