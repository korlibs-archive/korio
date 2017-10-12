package com.soywiz.korio

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.EventLoopFactory
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.ws.WebSocketClient
import com.soywiz.korio.net.ws.WebSocketClientFactory
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.browser.window
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
	actual val platformName: String by lazy {
		if (jsTypeOf(window) === undefined) {
			"node.js"
		} else {
			"js"
		}
	}

	actual val osName: String by lazy {
		// navigator.platform
		"unknown"
	}

	actual val tmpdir: String = "/tmp"

	actual val localVfsProvider: LocalVfsProvider
		get() = TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

	actual val File_separatorChar: Char = '/'

	actual val asyncSocketFactory: AsyncSocketFactory
		get() = TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

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

	actual object DefaultHttpFactoryFactory {
		actual fun createFactory(): HttpFactory = object : HttpFactory {
			override fun createClient(): HttpClient {
				return if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
			}

			override fun createServer(): HttpServer = HttpSeverNodeJs()
		}
	}

	actual val ResourcesVfs: VfsFile get() = UrlVfs(".")

	actual fun enterDebugger() {
		js("debugger;")
	}

	actual fun log(msg: Any?): Unit {
		console.log(msg)
	}

	actual fun error(msg: Any?): Unit {
		console.error(msg)
	}

	actual fun currentTimeMillis() = Date().getTime().toLong()

	actual fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) {
		for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
	}

	actual fun <T> fill(src: Array<T>, value: T, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

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

	actual class UTCDate(private val date: dynamic) {
		companion actual object {
			actual operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate {
				return UTCDate(window["Date"].asDynamic().UTC(fullYear, month0, day, hours, minutes, seconds))
			}

			actual operator fun invoke(time: Long): UTCDate {
				return UTCDate(Date(time.toDouble()).asDynamic())
			}
		}

		actual val time: Long get() = (date.getTime() as Double).toLong()
		actual val fullYear: Int get() = date.getUTCFullYear()
		actual val dayOfMonth: Int get() = date.getUTCDate()
		actual val dayOfWeek: Int get() = date.getUTCDay()
		actual val month0: Int get() = date.getUTCMonth()
		actual val hours: Int get() = date.getUTCHours()
		actual val minutes: Int get() = date.getUTCMinutes()
		actual val seconds: Int get() = date.getUTCSeconds()
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
	// @TODO: Implement in nodejs!
}

fun jsRequire(name: String) = global.require(name)

class HttpClientNodeJs : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = Promise.create { deferred ->
		//println(url)

		val http = jsRequire("http")
		val jsurl = jsRequire("url")
		val info = jsurl.call("parse", url)
		val reqHeaders = js("{}")

		for (header in headers) reqHeaders[header.first] = header.second

		val req = js("{}")
		req.method = method.name
		req.host = info["hostname"]
		req.port = info["port"]
		req.path = info["path"]
		req.agent = false
		req.encoding = null
		req.headers = reqHeaders

		val r = http.request(req, { res ->
			val statusCode = res.statusCode.toInt()
			val statusMessage = res.statusMessage.toJavaStringOrNull() ?: ""
			val jsHeadersObj = res.headers
			val body = js("[]")
			res.call("on", "data", { d -> body.call("push", d) })
			res.call("on", "end", {
				val r = global["Buffer"].call("concat", body)
				val u8array = Int8Array(r as ArrayBuffer)
				val out = ByteArray(u8array.length)
				for (n in 0 until u8array.length) out[n] = u8array[n]

				deferred.resolve(Response(
					status = statusCode,
					statusText = statusMessage,
					headers = Http.Headers(
						lmapOf()
						//(jsHeadersObj?.toObjectMap() ?: lmapOf()).mapValues { it.value.toJavaStringOrNull() ?: "" }
					),
					content = out.openAsync()
				))
			})
		}).on("error", { e ->
			deferred.reject(kotlin.RuntimeException("Error: ${e.toJavaString()}"))
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


/*
class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val stat = fstat(path)
			val handle = open(path, "r")

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val data = read(handle, position.toDouble(), len.toDouble())
					data.copyRangeTo(0, buffer, offset, data.size)
					return data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = try {
			val stat = fstat(path)
			createExistsStat(path, isDirectory = stat.isDirectory, size = stat.size.toLong())
		} catch (t: Throwable) {
			createNonExistsStat(path)
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> {
			val emitter = AsyncSequenceEmitter<VfsFile>()
			val fs = jsRequire("fs")
			//console.methods["log"](path)
			fs.call("readdir", path, jsFunctionRaw2 { err, files ->
				//console.methods["log"](err)
				//console.methods["log"](files)
				for (n in 0 until files["length"].toInt()) {
					val file = files[n].toJavaString()
					//println("::$file")
					emitter(file("$path/$file"))
				}
				emitter.close()
			})
			return emitter.toSequence()
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
			val fs = jsRequire("fs")
			val watcher = fs.call("watch", path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
				spawnAndForget(this@withCoroutineContext) {
					val et = eventType.toJavaString()
					val fn = filename.toJavaString()
					val f = file("$path/$fn")
					//println("$et, $fn")
					when (et) {
						"rename" -> {
							val kind = if (f.exists()) VfsFileEvent.Kind.CREATED else VfsFileEvent.Kind.DELETED
							handler(VfsFileEvent(kind, f))
						}
						"change" -> {
							handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, f))
						}
						else -> {
							println("Unhandled event: $et")
						}
					}
				}
			})

			return@withCoroutineContext Closeable { watcher.call("close") }
		}

		override fun toString(): String = "LocalVfs"
	}


	suspend fun open(path: String, mode: String): JsDynamic = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.call("open", path, mode, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				c.resume(fd!!)
			}
		})
	}

	suspend fun read(fd: JsDynamic?, position: Double, len: Double): ByteArray = Promise.create { c ->
		val fs = jsRequire("fs")
		val buffer = jsNew("Buffer", len)
		fs.call("read", fd, buffer, 0, len, position, jsFunctionRaw3 { err, bytesRead, buffer ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening ${fd.toJavaString()}"))
			} else {
				val u8array = jsNew("Int8Array", buffer, 0, bytesRead)
				val out = ByteArray(bytesRead.toInt())
				out.asJsDynamic().call("setArraySlice", 0, u8array)
				c.resolve(out)
			}
		})
	}

	suspend fun close(fd: Any): Unit = Promise.create { c ->
		val fs = jsRequire("fs")
		fs.call("close", fd, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} closing file"))
			} else {
				c.resolve(Unit)
			}
		})
	}



	suspend fun fstat(path: String): JsStat = Promise.create { c ->
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
		val fs = jsRequire("fs")
		//fs.methods["exists"](path, jsFunctionRaw1 { jsexists ->
		//	val exists = jsexists.toBool()
		//	if (exists) {
		fs.call("stat", path, jsFunctionRaw2 { err, stat ->
			//console.methods["log"](stat)
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				val out = JsStat(stat["size"].toDouble())
				out.isDirectory = stat.call("isDirectory").toBool()
				c.resolve(out)
			}
		})
		//	} else {
		//		c.resumeWithException(RuntimeException("File '$path' doesn't exists"))
		//	}
		//})
	}
}

class ResourcesVfsProviderJs : ResourcesVfsProvider() {
	override fun invoke(): Vfs {
		return EmbededResourceListing(if (OS.isNodejs) {
			LocalVfs(getCWD())
		} else {
			UrlVfs(getBaseUrl())
		}.jail())
	}

	private fun getCWD(): String = global["process"].call("cwd").toJavaString()

	private fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].call("replace", jsRegExp("/[^\\/]*$"), "")
		val bases = document.call("getElementsByTagName", "base")
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"]
		return baseHref.toJavaString()
	}
}

@Suppress("unused")
private class EmbededResourceListing(parent: VfsFile) : Vfs.Decorator(parent) {
	val nodeVfs = NodeVfs()

	init {
		for (asset in jsGetAssetStats()) {
			val info = PathInfo(asset.path.trim('/'))
			val folder = nodeVfs.rootNode.access(info.folder, createFolders = true)
			folder.createChild(info.basename, isDirectory = false).data = asset.size
		}
	}

	suspend override fun stat(path: String): VfsStat {
		try {
			val n = nodeVfs.rootNode[path]
			return createExistsStat(path, n.isDirectory, n.data as Long)
		} catch (t: Throwable) {
			return createNonExistsStat(path)
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = withCoroutineContext {
		asyncGenerate(this@withCoroutineContext) {
			for (item in nodeVfs.rootNode[path]) {
				yield(file("$path/${item.name}"))
			}
		}
	}

	override fun toString(): String = "ResourcesVfs[$parent]"
}
*/