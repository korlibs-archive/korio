package com.soywiz.korio.net.http

import com.soywiz.kds.Extra
import com.soywiz.kds.lmapOf
import com.soywiz.korio.IOException
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.asyncGenerate3
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.querystring.QueryString
import com.soywiz.korio.stream.AsyncOutputStream
import com.soywiz.korio.stream.ByteArrayBuilder
import com.soywiz.korio.stream.EMPTY_BYTE_ARRAY
import com.soywiz.korio.util.AsyncCloseable
import kotlin.coroutines.experimental.suspendCoroutine


open class HttpServer protected constructor() : AsyncCloseable {
	companion object {
		operator fun invoke() = defaultHttpFactory.createServer()
	}

	abstract class BaseRequest(
		val uri: String,
		val headers: Http.Headers
	) : Extra by Extra.Mixin() {
		private val parts by lazy { uri.split('?', limit = 2) }
		val path: String by lazy { parts[0] }
		val queryString: String by lazy { parts.getOrElse(1) { "" } }
		val getParams by lazy { QueryString.decode(queryString) }
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

	val requestConfig = RequestConfig()

	data class RequestConfig(
		val beforeSendHeadersInterceptors: MutableMap<String, suspend (Request) -> Unit> = lmapOf()
	) : Extra by Extra.Mixin() {
		// TODO:
		fun registerComponent(component: Any, dependsOn: List<Any>): Unit = TODO()
	}

	abstract class Request constructor(
		val method: Http.Method,
		uri: String,
		headers: Http.Headers,
		val requestConfig: RequestConfig = RequestConfig()
	) : BaseRequest(uri, headers), AsyncOutputStream {
		val finalizers = arrayListOf<suspend () -> Unit>()

		fun getHeader(key: String): String? = headers[key]

		fun getHeaderList(key: String): List<String> = headers.getAll(key)

		private var headersSent = false
		private var finalizingHeaders = false
		private val resHeaders = ArrayList<Pair<String, String>>()
		private var code: Int = 200
		private var message: String = "OK"

		private fun ensureHeadersNotSent() {
			if (headersSent) {
				println("Sent headers: $resHeaders")
				throw IOException("Headers already sent")
			}
		}

		fun removeHeader(key: String) {
			ensureHeadersNotSent()
			resHeaders.removeAll { it.first.equals(key, ignoreCase = true) }
		}

		fun addHeader(key: String, value: String) {
			ensureHeadersNotSent()
			resHeaders += key to value
		}

		fun replaceHeader(key: String, value: String) {
			ensureHeadersNotSent()
			removeHeader(key)
			addHeader(key, value)
		}

		abstract suspend protected fun _handler(handler: (ByteArray) -> Unit)
		abstract suspend protected fun _endHandler(handler: () -> Unit)
		abstract suspend protected fun _sendHeader(code: Int, message: String, headers: Http.Headers)
		abstract suspend protected fun _write(data: ByteArray, offset: Int = 0, size: Int = data.size - offset)
		abstract suspend protected fun _end()

		suspend fun handler(handler: (ByteArray) -> Unit) {
			_handler(handler)
		}

		suspend fun endHandler(handler: () -> Unit) {
			_endHandler(handler)
		}

		suspend fun readRawBody(maxSize: Int = 0x1000): ByteArray = suspendCoroutine { c ->
			val out = ByteArrayBuilder()
			spawnAndForget(c.context) {
				handler {
					if (out.size + it.size > maxSize) {
						out.clear()
					} else {
						out.append(it)
					}
				}
				endHandler {
					c.resume(out.toByteArray())
				}
			}
		}

		fun setStatus(code: Int, message: String = HttpStatusMessage(code)) {
			ensureHeadersNotSent()
			this.code = code
			this.message = message
		}

		private suspend fun flushHeaders() {
			//println("flushHeaders")
			if (headersSent) return
			if (finalizingHeaders) invalidOp("Can't write while finalizing headers")
			finalizingHeaders = true
			for (interceptor in requestConfig.beforeSendHeadersInterceptors) {
				interceptor.value(this)
			}
			headersSent = true
			//println("----HEADERS-----\n" + resHeaders.joinToString("\n"))
			_sendHeader(this.code, this.message, Http.Headers(resHeaders))
		}

		override suspend fun write(buffer: ByteArray, offset: Int, len: Int) {
			flushHeaders()
			_write(buffer, offset, len)
		}

		suspend fun end() {
			//println("END")
			flushHeaders()
			_end()
			for (finalizer in finalizers) finalizer()
		}

		suspend fun end(data: ByteArray) {
			replaceHeader("Content-Length", "${data.size}")
			flushHeaders()
			_write(data, 0, data.size)
			end()
		}

		suspend fun write(data: String, charset: Charset = Charsets.UTF_8) {
			flushHeaders()
			_write(data.toByteArray(charset))
		}

		suspend fun end(data: String, charset: Charset = Charsets.UTF_8) {
			end(data.toByteArray(charset))
		}

		suspend override fun close() {
			end()
		}
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
	val body: ByteArray = EMPTY_BYTE_ARRAY,
	requestConfig: HttpServer.RequestConfig
) : HttpServer.Request(method, uri, headers, requestConfig) {
	private val buf = ByteArrayBuilder()
	var outputHeaders: Http.Headers = Http.Headers()
	var outputStatusCode: Int = 0
	var outputStatusMessage: String = ""
	var output: String = ""
	val log = arrayListOf<String>()

	override suspend fun _handler(handler: (ByteArray) -> Unit) {
		log += "_handler()"
		handler(body)
	}

	override suspend fun _endHandler(handler: () -> Unit) {
		log += "_endHandler()"
		handler()
	}

	override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
		log += "_setStatus($code, $message)"
		outputStatusCode = code
		outputStatusMessage = message
		log += "_sendHeaders($headers)"
		outputHeaders = headers
	}

	override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
		log += "_write(${data.copyOfRange(offset, offset + size).toString(Charsets.UTF_8)})"
		buf.append(data, offset, size)
	}

	override suspend fun _end() {
		log += "_end()"
		output = buf.toByteArray().toString(Charsets.UTF_8)
	}

	override fun toString(): String = "$outputStatusCode:$outputStatusMessage:$outputHeaders:$output"
}
