package com.soywiz.korio

import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.SuspendingSequence
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.AsyncServer
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.stream.*
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsUtil
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

external internal fun require(name: String): dynamic

typealias NodeBuffer = Uint8Array

fun NodeBuffer.toByteArray() = Int8Array(this.unsafeCast<Int8Array>()).unsafeCast<ByteArray>()
//fun ByteArray.toNodeJsBufferU8(): NodeBuffer = Uint8Array(this.unsafeCast<ArrayBuffer>()).asDynamic()

fun ByteArray.asInt8Array(): Int8Array = this.unsafeCast<Int8Array>()
fun ByteArray.asUint8Array(): Uint8Array {
	val i = this.asInt8Array()
	return Uint8Array(i.buffer, i.byteOffset, i.length)
}
fun ByteArray.toNodeJsBuffer(): NodeBuffer = this.asUint8Array().unsafeCast<NodeBuffer>()
fun ByteArray.toNodeJsBuffer(offset: Int, size: Int): NodeBuffer = global.Buffer.from(this, offset, size).unsafeCast<NodeBuffer>()

class HttpClientNodeJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
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

		val r = http.request(req, { res ->
			val statusCode: Int = res.statusCode
			val statusMessage: String = res.statusMessage ?: ""
			val jsHeadersObj = res.headers
			val body = jsEmptyArray()
			res.on("data", { d -> body.push(d) })
			res.on("end", {
				val r = global.Buffer.concat(body)
				val u8array = Int8Array(r.unsafeCast<ArrayBuffer>())
				val out = ByteArray(u8array.length)
				for (n in 0 until u8array.length) out[n] = u8array[n]
				val response = Response(
					status = statusCode,
					statusText = statusMessage,
					headers = Http.Headers(
						(jsToObjectMap(jsHeadersObj) ?: lmapOf()).mapValues { "${it.value}" }
					),
					content = out.openAsync()
				)

				//println(response.headers)

				deferred.resolve(response)
			})
		}).on("error", { e ->
			deferred.reject(kotlin.RuntimeException("Error: $e"))
		})

		deferred.onCancel {
			r.abort()
		}

		if (content != null) {
			r.end(content.readAll().toTypedArray())
		} else {
			r.end()
		}
		Unit
	}
}

class HttpSeverNodeJs : HttpServer() {
	private var context: CoroutineContext = EmptyCoroutineContext
	private var handler: suspend (req: dynamic, res: dynamic) -> Unit = { req, res -> }

	val http = require("http")
	val server = http.createServer { req, res ->
		spawnAndForget(context) {
			handler(req, res)
		}
	}

	suspend override fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
		super.websocketHandlerInternal(handler)
	}

	suspend override fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
		context = getCoroutineContext()
		this.handler = { req, res ->
			// req: https://nodejs.org/api/http.html#http_class_http_incomingmessage
			// res: https://nodejs.org/api/http.html#http_class_http_serverresponse

			val method = Http.Method[req.method.unsafeCast<String>()]
			val url = req.url.unsafeCast<String>()
			val headers = Http.Headers(jsToArray(req.rawHeaders).map { "$it" }.zipWithNext())
			handler(object : Request(method, url, headers, RequestConfig()) {
				suspend override fun _handler(handler: (ByteArray) -> Unit) {
					req.on("data") { chunk ->
						handler(Int8Array(chunk.unsafeCast<Uint8Array>().buffer).unsafeCast<ByteArray>())
					}
				}

				suspend override fun _endHandler(handler: () -> Unit) {
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

	suspend override fun listenInternal(port: Int, host: String) = suspendCoroutine<Unit> { c ->
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

	suspend override fun closeInternal() = suspendCoroutine<Unit> { c ->
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

	suspend override fun connect(host: String, port: Int): Unit = suspendCoroutine { c ->
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

	suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int {
		connection?.resume()
		try {
			return input.read(buffer, offset, len)
		} finally {
			connection?.pause()
		}
	}

	suspend override fun write(buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
		connection?.write(buffer.toNodeJsBuffer(offset, len)) {
			c.resume(Unit)
		}
		Unit
	}

	suspend override fun close() {
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

	suspend override fun listen(handler: suspend (AsyncClient) -> Unit): Closeable {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	suspend override fun listen(): SuspendingSequence<AsyncClient> {
		return super.listen()
	}

	suspend fun init(port: Int, host: String, backlog: Int): AsyncServer = this.apply {
	}
}


class NodeJsLocalVfs(val base: String) : LocalVfs() {
	val fs = require("fs")

	interface FD

	private fun getFullPath(path: String): String {
		return base + "/" + VfsUtil.normalize(path)
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean = suspendCoroutine { c ->
		fs.mkdir(getFullPath(path), "777".toInt(8)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	suspend override fun rename(src: String, dst: String): Boolean = suspendCoroutine { c ->
		fs.rename(getFullPath(src), getFullPath(dst)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	suspend override fun delete(path: String): Boolean = suspendCoroutine { c ->
		fs.unlink(getFullPath(path)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	suspend override fun rmdir(path: String): Boolean = suspendCoroutine { c ->
		fs.rmdir(getFullPath(path)) { err ->
			c.resume((err == null))
			Unit
		}
		Unit
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
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
}

class NodeFDStream(val fs: dynamic, val fd: NodeJsLocalVfs.FD) : AsyncStreamBase() {
	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = suspendCoroutine { c ->
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

	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = suspendCoroutine { c ->
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

	suspend override fun setLength(value: Long): Unit = suspendCoroutine { c ->
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

	suspend override fun getLength(): Long = suspendCoroutine { c ->
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

	suspend override fun close(): Unit = suspendCoroutine { c ->
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
