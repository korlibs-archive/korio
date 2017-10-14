package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.async.Promise
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.net.AsyncClient
import com.soywiz.korio.net.AsyncServer
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.WebSocketClientFactory
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.browser.window
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.EmptyCoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.js.*
import kotlin.reflect.KClass

actual annotation class Synchronized
actual annotation class JvmField
actual annotation class JvmStatic
actual annotation class JvmOverloads
actual annotation class Transient

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual open class RuntimeException actual constructor(msg: String) : Exception(msg)
actual open class IllegalStateException actual constructor(msg: String) : RuntimeException(msg)
actual open class CancellationException actual constructor(msg: String) : IllegalStateException(msg)

val global = js("(typeof global !== 'undefined') ? global : window")

actual object KorioNative {
	actual val currentThreadId: Long = 1L

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T
		private var value = initialValue()
		actual fun get(): T = value
		actual fun set(value: T) = run { this.value = value }
	}

	actual val platformName: String by lazy {
		if (jsTypeOf(window) === "undefined") {
			"node.js"
		} else {
			"js"
		}
	}

	actual val osName: String by lazy {
		// navigator.platform
		"unknown"
	}

	actual fun getLocalTimezoneOffset(time: Long): Int {
		@Suppress("UNUSED_VARIABLE")
		val rtime = time.toDouble()
		return js("-(new Date(rtime)).getTimezoneOffset()")
	}

	actual val tmpdir: String = "/tmp"

	actual val localVfsProvider: LocalVfsProvider
		get() = TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

	actual val File_separatorChar: Char = '/'

	actual val asyncSocketFactory: AsyncSocketFactory by lazy {
		object : AsyncSocketFactory() {
			suspend override fun createClient(): AsyncClient = NodeJsAsyncClient()
			suspend override fun createServer(port: Int, host: String, backlog: Int): AsyncServer = NodeJsAsyncServer().init(port, host, backlog)
		}
	}

	actual val websockets: WebSocketClientFactory by lazy { JsWebSocketClientFactory() }

	actual val eventLoopFactoryDefaultImpl: EventLoopFactory = EventLoopFactoryJs()

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T {
		return callback()
	}

	actual fun Thread_sleep(time: Long) {}

	actual class Inflater actual constructor(val nowrap: Boolean) {
		actual fun needsInput(): Boolean = TODO()
		actual fun setInput(buffer: ByteArray): Unit = TODO()
		actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = TODO()
		actual fun end(): Unit = TODO()
	}

	actual object SyncCompression {
		actual fun inflate(data: ByteArray): ByteArray {
			TODO()
		}

		actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
			TODO()
		}

		actual fun deflate(data: ByteArray, level: Int): ByteArray {
			TODO()
		}
	}

	actual class SimplerMessageDigest actual constructor(name: String) {
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
		actual suspend fun digest(): ByteArray = TODO()
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
		actual suspend fun finalize(): ByteArray = TODO()
	}

	actual class NativeCRC32 {
		actual fun update(data: ByteArray, offset: Int, size: Int): Unit = TODO()
		actual fun digest(): Int = TODO()
	}

	actual object CreateAnnotation {
		actual fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T = TODO()
	}

	actual val httpFactory: HttpFactory by lazy {
		object : HttpFactory {
			override fun createClient(): HttpClient = if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
			override fun createServer(): HttpServer = HttpSeverNodeJs()
		}
	}

	actual val ResourcesVfs: VfsFile get() = UrlVfs(".")

	actual fun enterDebugger() {
		js("debugger;")
	}

	actual fun printStackTrace(e: Throwable) {
		console.error(e.asDynamic())
		console.error(e.asDynamic().stack)

		// @TODO: Implement in each platform!

		//e.printStackTrace()
	}

	actual fun log(msg: Any?): Unit {
		console.log(msg)
	}

	actual fun error(msg: Any?): Unit {
		console.error(msg)
	}

	actual fun currentTimeMillis() = Date().getTime().toLong()

	actual fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)

	actual fun <T> fill(src: Array<T>, value: T, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)

	suspend actual fun uncompressGzip(data: ByteArray): ByteArray = TODO()
	suspend actual fun uncompressZlib(data: ByteArray): ByteArray = TODO()
	suspend actual fun compressGzip(data: ByteArray, level: Int): ByteArray = TODO()
	suspend actual fun compressZlib(data: ByteArray, level: Int): ByteArray = TODO()

	actual class FastMemory(val buffer: Uint8Array, actual val size: Int) {
		val data = DataView(buffer.buffer)
		val i16 = Int16Array(buffer.buffer)
		val i32 = Int32Array(buffer.buffer)
		val f32 = Float32Array(buffer.buffer)

		companion actual object {
			actual fun alloc(size: Int): FastMemory {
				return FastMemory(Uint8Array((size + 0xF) and 0xF.inv()), size)
			}

			actual fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit {
				//dst.buffer.slice()
				//dst.buffer.position(srcPos)
				//dst.buffer.put(src.buffer, srcPos, length)

				// COPY
				for (n in 0 until length) dst[dstPos + n] = src[srcPos + n]
			}
		}

		actual operator fun get(index: Int): Int {
			return buffer[index].toInt() and 0xFF
		}

		actual operator fun set(index: Int, value: Int): Unit {
			buffer[index] = value.toByte()
		}

		actual fun setAlignedInt16(index: Int, value: Short) = run { i16[index] = value }
		actual fun getAlignedInt16(index: Int): Short = i16[index]
		actual fun setAlignedInt32(index: Int, value: Int) = run { i32[index] = value }
		actual fun getAlignedInt32(index: Int): Int = i32[index]
		actual fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32[index] = value }
		actual fun getAlignedFloat32(index: Int): Float = f32[index]

		actual fun setAlignedArrayInt8(index: Int, data: ByteArray, offset: Int, len: Int) {
			for (n in 0 until len) set(index + n, data[offset + n].toInt() and 0xFF)
		}

		actual fun setAlignedArrayInt16(index: Int, data: ShortArray, offset: Int, len: Int) {
			for (n in 0 until len) setAlignedInt16(index + n, data[offset + n])
		}

		actual fun setAlignedArrayInt32(index: Int, data: IntArray, offset: Int, len: Int) {
			for (n in 0 until len) setAlignedInt32(index + n, data[offset + n])
		}

		actual fun setAlignedArrayFloat32(index: Int, data: FloatArray, offset: Int, len: Int) {
			for (n in 0 until len) setAlignedFloat32(index + n, data[offset + n])
		}

		actual fun getInt16(index: Int): Short = data.getInt16(index)
		actual fun getInt32(index: Int): Int = data.getInt32(index)
		actual fun getFloat32(index: Int): Float = data.getFloat32(index)
	}
}

external private class Date(time: Double)


private class EventLoopFactoryJs : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopJs()
}

@Suppress("unused")
private class EventLoopJs : EventLoop() {
	val immediateHandlers = LinkedList<() -> Unit>()
	var insideImmediate = false

	override fun setImmediateInternal(handler: () -> Unit) {
		//println("setImmediate")
		immediateHandlers += handler
		if (!insideImmediate) {
			insideImmediate = true
			try {
				while (immediateHandlers.isNotEmpty()) {
					val fhandler = immediateHandlers.removeFirst()
					fhandler()
				}
			} finally {
				insideImmediate = false
			}
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val id = window.setTimeout({ callback() }, ms)
		//println("setTimeout($ms)")
		return Closeable { global.clearInterval(id) }
	}

	override fun requestAnimationFrameInternal(callback: () -> Unit): Closeable {
		val id = global.requestAnimationFrame { callback() }
		//println("setTimeout($ms)")
		return Closeable { global.cancelAnimationFrame(id) }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		//println("setInterval($ms)")
		val id = global.setInterval({ callback() }, ms)
		return Closeable { global.clearInterval(id) }
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

//@JsName("require")
external private fun require(name: String): dynamic


fun ByteArray.toNodeJsBuffer(offset: Int, size: Int): dynamic {
	return global.Buffer.from(this, offset, size)
}

fun jsNew(clazz: dynamic): dynamic = js("(new (clazz))()")
fun jsNew(clazz: dynamic, a0: dynamic): dynamic = js("(new (clazz))(a0)")
fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic): dynamic = js("(new (clazz))(a0, a1)")
fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic, a2: dynamic): dynamic = js("(new (clazz))(a0, a1, a2)")
fun jsEnsureNumber(v: dynamic): Number = js("+v")
fun jsEnsureInt(v: dynamic): Int = js("v|0")
fun jsEmptyObj(): dynamic = js("({})")
fun jsEmptyArray(): dynamic = js("([])")
fun jsObjectKeys(obj: dynamic): dynamic = js("Object.keys(obj)")
fun jsToArray(obj: dynamic): Array<Any?> = Array<Any?>(obj.length) { obj[it] }

fun jsToObjectMap(obj: dynamic): Map<String, Any?>? {
	if (obj == null) return null
	val out = lmapOf<String, Any?>()
	val keys = jsObjectKeys(obj)
	for (n in 0 until keys.length) {
		val key = keys[n]
		out["$key"] = obj[key]
	}
	return out
}

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

class HttpClientBrowserJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		val xhr = XMLHttpRequest()
		xhr.open(method.name, url, true)
		xhr.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

		xhr.onload = { e ->
			val u8array = Uint8Array(xhr.response as ArrayBuffer)
			val out = ByteArray(u8array.length)
			for (n in out.indices) out[n] = u8array[n]
			//js("debugger;")
			deferred.resolve(Response(
				status = xhr.status.toInt(),
				statusText = xhr.statusText,
				headers = Http.Headers(xhr.getAllResponseHeaders()),
				content = out.openAsync()
			))
		}

		xhr.onerror = { e ->
			deferred.reject(kotlin.RuntimeException("Error ${xhr.status} opening $url"))
		}

		for (header in headers) xhr.setRequestHeader(header.first, header.second)

		deferred.onCancel { xhr.abort() }

		if (content != null) {
			xhr.send(content.readAll())
		} else {
			xhr.send()
		}
	}
}


class JsWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(url: String, protocols: List<String>?, origin: String?, wskey: String?, debug: Boolean): WebSocketClient = JsWebSocketClient(url, protocols, DEBUG = debug).apply { init() }
}

class JsWebSocketClient(url: String, protocols: List<String>?, val DEBUG: Boolean) : WebSocketClient(url, protocols, true) {
	val jsws = if (protocols != null) {
		WebSocket(url, arrayOf(*protocols.toTypedArray()))
	} else {
		WebSocket(url)
	}.apply {
		this.binaryType = BinaryType.ARRAYBUFFER
		this.addEventListener("open", { onOpen(Unit) })
		this.addEventListener("close", { e ->
			val event = e as CloseEvent
			var code = event.code.toInt()
			var reason = event.reason
			var wasClean = event.wasClean
			onClose(Unit)
		})
		this.addEventListener("message", { e ->
			val event = e as MessageEvent
			val data = event.data
			if (DEBUG) println("[WS-RECV]: $data :: stringListeners=${onStringMessage.listenerCount}, binaryListeners=${onBinaryMessage.listenerCount}, anyListeners=${onAnyMessage.listenerCount}")
			if (data is String) {
				val js = data
				onStringMessage(js)
				onAnyMessage(js)
			} else {
				val jb = data

				//onBinaryMessage(jb)
				//onAnyMessage(jb)
				TODO("onBinaryMessage, onAnyMessage")
			}
		})
	}

	suspend fun init() {
		if (DEBUG) println("[WS] Wait connection ($url)...")
		onOpen.waitOne()
		if (DEBUG) println("[WS] Connected!")
	}

	override fun close(code: Int, reason: String) {
		//jsws.methods["close"](code, reason)
		jsws.close()
	}

	override suspend fun send(message: String) {
		if (DEBUG) println("[WS-SEND]: $message")
		jsws.send(message)
	}

	override suspend fun send(message: ByteArray) {
		if (DEBUG) println("[WS-SEND]: ${message.toList()}")
		val bb = Int8Array(message.size)
		for (n in message.indices) bb[n] = message[n]
		jsws.send(bb)
	}
}

data class JsStat(val size: Double, var isDirectory: Boolean = false) {
	fun toStat(path: String, vfs: Vfs): VfsStat = vfs.createExistsStat(path, isDirectory = isDirectory, size = size.toLong())
}

private class NodeJsAsyncClient : AsyncClient {
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
