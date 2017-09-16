package com.soywiz.korio.vertx

import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readBytes
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import java.net.URL

class VertxHttpClientFactory : HttpFactory() {
	override val available: Boolean = true
	override val priority: Int = 500

	override fun createClient(): HttpClient = VertxHttpClient()
	override fun createServer(): HttpServer = VertxHttpServer()
}

class VertxHttpServer : HttpServer() {
	val vxServer = vertx.createHttpServer()

	override val actualPort: Int get() = vxServer.actualPort()

	suspend override fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
		val ctx = getCoroutineContext()

		vxServer.websocketHandler { ws ->
			val kws = object : WsRequest(
					uri = ws.uri(),
					headers = Http.Headers(ws.headers().map { it.key to it.value })
			) {
				override fun reject() {
					ws.reject()
				}

				override fun close() {
					ws.close()
				}

				override fun onStringMessage(handler: suspend (String) -> Unit) {
					ws.textMessageHandler {
						ctx.eventLoop.queue {
							go(ctx) {

								handler(it)
							}
						}
					}
				}

				override fun onBinaryMessage(handler: suspend (ByteArray) -> Unit) {
					ws.binaryMessageHandler {
						ctx.eventLoop.queue {
							go(ctx) {
								handler(it.bytes)
							}
						}
					}
				}

				override fun onClose(handler: suspend () -> Unit) {
					ws.closeHandler {
						go(ctx) {
							handler()
						}
					}
				}

				override fun send(msg: String) {
					ws.writeFinalTextFrame(msg)
				}

				override fun send(msg: ByteArray) {
					ws.writeFinalBinaryFrame(Buffer.buffer(msg))
				}
			}
			go(ctx) {
				handler(kws)
			}
		}
	}

	suspend override fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
		val ctx = getCoroutineContext()

		vxServer.requestHandler { req ->
			val res = req.response()
			val kreq = object : Request(
					method = Http.Method(req.rawMethod()),
					uri = req.uri(),
					headers = Http.Headers(req.headers().map { it.key to it.value })
			) {
				override fun _handler(handler: (ByteArray) -> Unit) {
					req.handler { handler(it.bytes) }
				}

				override fun _endHandler(handler: () -> Unit) {
					req.endHandler { handler() }
				}

				override fun _setStatus(code: Int, message: String) {
					res.statusCode = code
					res.statusMessage = message
				}

				override fun _sendHeaders(headers: Http.Headers) {
					for (header in headers) {
						res.putHeader(header.first, header.second)
					}
				}

				override fun _emit(data: ByteArray) {
					res.write(Buffer.buffer(data))
				}

				override fun _end() {
					res.end()
				}
			}
			go(ctx) {
				handler(kreq)
			}
		}
	}

	suspend override fun listenInternal(port: Int, host: String) {
		val wait = Promise.Deferred<Unit>()
		vxServer.listen(port, host) {
			if (it.failed()) {
				wait.reject(it.cause())
			} else {
				wait.resolve(Unit)
			}
		}
		wait.promise.await()
	}

	suspend override fun closeInternal() {
		vxServer.close()
	}
}

class VertxHttpClient : HttpClient() {
	val vxMethods = HttpMethod.values().map { it.name to it }.toMap()
	val krMethods = Http.Method.values().map { it.name to it }.toMap()
	val convertMethod = krMethods.values.map { it to vxMethods[it.name] }.toMap()

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		val urlUrl = URL(url)
		val ssl = when (urlUrl.protocol) {
			"https", "wss" -> true
			else -> false
		}
		val vxMethod = convertMethod[method]!!
		val vxclient = vertx.createHttpClient(HttpClientOptions().setSsl(ssl))
		val req = vxclient.requestAbs(vxMethod, url)
		for ((k, v) in headers) req.putHeader(k, v)

		val p = ProduceConsumer<ByteArray>()

		var resolved = false
		val deferred = Promise.Deferred<HttpClientResponse>()

		req.handler { res ->
			fun checkSent() {
				if (!resolved) {
					resolved = true
					deferred.resolve(res)
				}
			}

			res.handler {
				checkSent()
				p.produce(it.bytes)
			}
			res.endHandler {
				checkSent()
				p.close()
				vxclient.close()
			}
		}

		if (content != null) {
			while (!content.eof()) {
				val data = content.readBytes(0x1000)
				req.write(Buffer.buffer(data))
			}
		}
		req.end()

		val res = deferred.promise.await()

		return Response(
				status = res.statusCode(),
				statusText = res.statusMessage(),
				headers = Http.Headers(res.headers().map { it.key to it.value }),
				content = p.toAsyncInputStream()
		)
	}
}
