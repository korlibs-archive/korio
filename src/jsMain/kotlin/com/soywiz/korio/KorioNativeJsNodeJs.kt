package com.soywiz.korio

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import kotlin.coroutines.*

internal external fun require(name: String): dynamic

typealias NodeJsBuffer = Uint8Array

fun NodeJsBuffer.toByteArray() = Int8Array(this.unsafeCast<Int8Array>()).unsafeCast<ByteArray>()
//fun ByteArray.toNodeJsBufferU8(): NodeBuffer = Uint8Array(this.unsafeCast<ArrayBuffer>()).asDynamic()

fun ByteArray.asInt8Array(): Int8Array = this.unsafeCast<Int8Array>()
fun ByteArray.asUint8Array(): Uint8Array {
	val i = this.asInt8Array()
	return Uint8Array(i.buffer, i.byteOffset, i.length)
}

fun ByteArray.toNodeJsBuffer(): NodeJsBuffer = this.asUint8Array().unsafeCast<NodeJsBuffer>()
fun ByteArray.toNodeJsBuffer(offset: Int, size: Int): NodeJsBuffer =
	global.asDynamic().Buffer.from(this, offset, size).unsafeCast<NodeJsBuffer>()

class HttpClientNodeJs : HttpClient() {
	override suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers,
		content: AsyncStream?
	): Response {
		val deferred = CompletableDeferred<Response>(Job())
		//println(url)

		val http = require("http")
		val jsurl = require("url")
		val info = jsurl.parse(url)
		val reqHeaders = jsEmptyObj()

		for (header in headers) {
			reqHeaders[header.first] = header.second
		}

		val req = jsEmptyObj()
		req.method = method.name
		req.host = info["hostname"]
		req.port = info["port"]
		req.path = info["path"]
		req.agent = false
		req.encoding = null
		req.headers = reqHeaders

		val r = http.request(req) { res ->
			val statusCode: Int = res.statusCode
			val statusMessage: String = res.statusMessage ?: ""
			val jsHeadersObj = res.headers
			val body = jsEmptyArray()
			res.on("data") { d -> body.push(d) }
			res.on("end") {
				val r = global.asDynamic().Buffer.concat(body)
				val u8array = Int8Array(r.unsafeCast<ArrayBuffer>())
				val out = ByteArray(u8array.length)
				for (n in 0 until u8array.length) out[n] = u8array[n]
				val response = Response(
					status = statusCode,
					statusText = statusMessage,
					headers = Http.Headers(
						(jsToObjectMap(jsHeadersObj) ?: LinkedHashMap()).mapValues { "${it.value}" }
					),
					content = out.openAsync()
				)

				//println(response.headers)

				deferred.complete(response)
			}
		}.on("error") { e ->
			deferred.completeExceptionally(kotlin.RuntimeException("Error: $e"))
		}

		deferred.invokeOnCompletion {
			if (deferred.isCancelled) {
				r.abort()
			}
		}

		if (content != null) {
			r.end(content.readAll().toTypedArray())
		} else {
			r.end()
		}

		return deferred.await()
	}
}

class HttpSeverNodeJs : HttpServer() {
	private var context: CoroutineContext = EmptyCoroutineContext
	private var handler: suspend (req: dynamic, res: dynamic) -> Unit = { req, res -> }

	val http = require("http")
	val server = http.createServer { req, res ->
		launchImmediately(context) {
			handler(req, res)
		}
	}

	override suspend fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
		super.websocketHandlerInternal(handler)
	}

	override suspend fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
		context = coroutineContext
		this.handler = { req, res ->
			// req: https://nodejs.org/api/http.html#http_class_http_incomingmessage
			// res: https://nodejs.org/api/http.html#http_class_http_serverresponse

			val method = Http.Method[req.method.unsafeCast<String>()]
			val url = req.url.unsafeCast<String>()
			val headers = Http.Headers(jsToArray(req.rawHeaders).map { "$it" }.zipWithNext())
			handler(object : Request(method, url, headers, RequestConfig()) {
				override suspend fun _handler(handler: (ByteArray) -> Unit) {
					req.on("data") { chunk ->
						handler(Int8Array(chunk.unsafeCast<Uint8Array>().buffer).unsafeCast<ByteArray>())
					}
				}

				override suspend fun _endHandler(handler: () -> Unit) {
					req.on("end") {
						handler()
					}
					req.on("error") {
						handler()
					}
				}

				override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
					res.statusCode = code
					res.statusMessage = message
					for (header in headers) {
						res.setHeader(header.first, header.second)
					}
				}

				override suspend fun _write(data: ByteArray, offset: Int, size: Int): Unit = suspendCoroutine { c ->
					res.write(data.toNodeJsBuffer(offset, size)) {
						c.resume(Unit)
					}
					Unit
				}

				override suspend fun _end(): Unit = suspendCoroutine { c ->
					res.end {
						c.resume(Unit)
					}
					Unit
				}
			})
		}
	}

	override suspend fun listenInternal(port: Int, host: String) = suspendCoroutine<Unit> { c ->
		context = c.context
		server.listen(port, host, 511) {
			c.resume(Unit)
		}
	}

	override val actualPort: Int
		get() {
			//com.soywiz.korio.lang.Console.log(server)
			return jsEnsureInt(server.address().port)
		}

	override suspend fun closeInternal() = suspendCoroutine<Unit> { c ->
		context = c.context
		server.close {
			c.resume(Unit)
		}
	}
}


class NodeJsAsyncClient : AsyncClient {
	private val net = require("net")
	private var connection: dynamic = null
	private val input = AsyncProduceConsumerByteBuffer()

	override var connected: Boolean = false; private set

	override suspend fun connect(host: String, port: Int): Unit = suspendCoroutine { c ->
		connection = net.createConnection(port, host) {
			connected = true
			connection?.pause()
			connection?.on("data") { it ->
				input.produce(it.unsafeCast<ByteArray>())
			}
			c.resume(Unit)
		}
		Unit
	}

	override suspend fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		connection?.resume()
		try {
			return input.read(buffer, offset, len)
		} finally {
			connection?.pause()
		}
	}

	override suspend fun write(buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
		connection?.write(buffer.toNodeJsBuffer(offset, len)) {
			c.resume(Unit)
		}
		Unit
	}

	override suspend fun close() {
		connection?.close()
	}
}

class NodeJsAsyncServer : AsyncServer {
	override val requestPort: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val host: String
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val backlog: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
	override val port: Int
		get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

	override suspend fun accept(): AsyncClient {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	suspend fun init(port: Int, host: String, backlog: Int): AsyncServer = this.apply {
	}
}


class NodeJsLocalVfs : LocalVfs() {
	val fs = require("fs")

	interface FD

	private fun getFullPath(path: String): String {
		return path.pathInfo.normalize()
	}

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean = suspendCoroutine { c ->
		fs.mkdir(getFullPath(path), "777".toInt(8)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun rename(src: String, dst: String): Boolean = suspendCoroutine { c ->
		fs.rename(getFullPath(src), getFullPath(dst)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun delete(path: String): Boolean = suspendCoroutine { c ->
		fs.unlink(getFullPath(path)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun rmdir(path: String): Boolean = suspendCoroutine { c ->
		fs.rmdir(getFullPath(path)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val cmode = when (mode) {
			VfsOpenMode.READ -> "r"
			VfsOpenMode.WRITE -> "r+"
			VfsOpenMode.CREATE_OR_TRUNCATE -> "w+"
			VfsOpenMode.CREATE_NEW -> {
				if (stat(path).exists) throw FileNotFoundException(path)
				"w+"
			}
			VfsOpenMode.CREATE -> "wx+"
			VfsOpenMode.APPEND -> "a"
		}

		return _open(path, cmode)
	}

	suspend fun _open(path: String, cmode: String): AsyncStream = suspendCoroutine { cc ->
		fs.open(getFullPath(path), cmode) { err, fd: FD ->
			if (err != null) {
				cc.resumeWithException(err)
			} else {
				cc.resume(NodeFDStream(fs, fd).toAsyncStream())
			}
			Unit
		}
		Unit
	}

	override fun toString(): String = "NodeJsLocalVfs"
}

class NodeFDStream(val fs: dynamic, val fd: NodeJsLocalVfs.FD) : AsyncStreamBase() {
	override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutine { c ->
		fs.read(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesRead, buffer ->
			if (err != null) {
				c.resumeWithException(err)
			} else {
				c.resume(bytesRead)
			}
			Unit
		}
		Unit
	}

	override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
		fs.write(fd, buffer.toNodeJsBuffer(), offset, len, position.toDouble()) { err, bytesWritten, buffer ->
			if (err != null) {
				c.resumeWithException(err)
			} else {
				c.resume(Unit)
			}
			Unit
		}
		Unit
	}

	override suspend fun setLength(value: Long): Unit = suspendCoroutine { c ->
		fs.ftruncate(fd, value.toDouble()) { err ->
			if (err != null) {
				c.resumeWithException(err)
			} else {
				c.resume(Unit)
			}
			Unit
		}
		Unit
	}

	override suspend fun getLength(): Long = suspendCoroutine { c ->
		fs.fstat(fd) { err, stats ->
			if (err != null) {
				c.resumeWithException(err)
			} else {
				c.resume((stats.size as Double).toLong())
			}
			Unit
		}
		Unit
	}

	override suspend fun close(): Unit = suspendCoroutine { c ->
		fs.close(fd) { err ->
			if (err != null) {
				c.resumeWithException(err)
			} else {
				c.resume(Unit)
			}
			Unit
		}
		Unit
	}
}
