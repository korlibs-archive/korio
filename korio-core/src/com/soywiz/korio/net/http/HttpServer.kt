package com.soywiz.korio.net.http

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.asyncGenerate3
import com.soywiz.korio.ds.OptByteBuffer
import com.soywiz.korio.util.AsyncCloseable
import com.soywiz.korio.util.Extra
import java.io.IOException


open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = defaultHttpFactory.createServer()
	}

	abstract class BaseRequest(
			val uri: String,
			val headers: Http.Headers
	) : Extra by Extra.Mixin() {
		val absoluteURI: String by lazy { uri }
	}

	abstract class WsRequest(
			uri: String,
			headers: Http.Headers
	) : BaseRequest(uri, headers) {
		abstract fun reject()

		abstract fun close()
		abstract fun onStringMessage(handler: suspend (String) -> Unit)
		abstract fun onBinaryMessage(handler: suspend (ByteArray) -> Unit)
		abstract fun onClose(handler: suspend () -> Unit)
		abstract fun send(msg: String)
		abstract fun send(msg: ByteArray)

		fun sendSafe(msg: String) {
			try {
				send(msg)
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		fun sendSafe(msg: ByteArray) {
			try {
				send(msg)
			} catch (e: Throwable) {
				e.printStackTrace()
			}
		}

		//suspend fun stringMessageStream(): SuspendingSequence<String> {
		//	val emitter = AsyncSequenceEmitter<String>()
		//	onStringMessage { emitter.emit(it) }
		//	onClose { emitter.close() }
		//	return emitter.toSequence()
		//}

		fun stringMessageStream() = asyncGenerate3<String> {
			onStringMessage { yield(it) }
			onClose { close() }
		}

		fun binaryMessageStream() = asyncGenerate3<ByteArray> {
			onBinaryMessage { yield(it) }
			onClose { close() }
		}

		fun anyMessageStream() = asyncGenerate3<Any> {
			onStringMessage { yield(it) }
			onBinaryMessage { yield(it) }
			onClose { close() }
		}
	}

	abstract class Request(
			val method: Http.Method,
			uri: String,
			headers: Http.Headers
	) : BaseRequest(uri, headers) {

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

class FakeRequest(
		method: Http.Method,
		uri: String,
		headers: Http.Headers = Http.Headers(),
		val body: ByteArray = ByteArray(0)
) : HttpServer.Request(method, uri, headers) {
	private val buf = OptByteBuffer()
	var outputHeaders: Http.Headers = Http.Headers()
	var outputStatusCode: Int = 0
	var outputStatusMessage: String = ""
	var output: String = ""

	override fun _handler(handler: (ByteArray) -> Unit) {
		handler(body)
	}

	override fun _endHandler(handler: () -> Unit) {
		handler()
	}

	override fun _setStatus(code: Int, message: String) {
		outputStatusCode = code
		outputStatusMessage = message
	}

	override fun _sendHeaders(headers: Http.Headers) {
		outputHeaders = headers
	}

	override fun _emit(data: ByteArray) {
		buf.append(data)
	}

	override fun _end() {
		output = buf.toByteArray().toString(Charsets.UTF_8)
	}

	override fun toString(): String {
		return "$outputStatusCode:$outputStatusMessage:$outputHeaders:$output"
	}
}
