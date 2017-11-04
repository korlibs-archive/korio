package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.crypto.Hex
import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.error.invalidOp
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
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.typedarray.copyRangeTo
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*
import com.soywiz.kzlib.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.browser.window
import kotlin.collections.set
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.math.min

actual annotation class Synchronized
actual annotation class JvmField
actual annotation class JvmStatic
actual annotation class JvmOverloads
actual annotation class Transient

actual annotation class Language actual constructor(actual val value: String, actual val prefix: String = "", actual val suffix: String = "")

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual open class RuntimeException actual constructor(msg: String) : Exception(msg)
actual open class IllegalStateException actual constructor(msg: String) : RuntimeException(msg)
actual open class CancellationException actual constructor(msg: String) : IllegalStateException(msg)

val global = js("(typeof global !== 'undefined') ? global : window")
external val process: dynamic // node.js
external val navigator: dynamic // browser

actual class Semaphore actual constructor(initial: Int) {
	//var initial: Int
	actual fun acquire() = Unit

	actual fun release() = Unit
}

val isNodeJs by lazy { jsTypeOf(window) === "undefined" }

actual object KorioNative {
	actual val currentThreadId: Long = 1L

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T
		private var value = initialValue()
		actual fun get(): T = value
		actual fun set(value: T) = run { this.value = value }
	}

	actual val platformName: String
		get() = when {
			isNodeJs -> "node.js"
			else -> "js"
		}

	actual val rawOsName: String = when {
		isNodeJs -> process.platform
		else -> navigator.platform
	}

	actual fun getRandomValues(data: ByteArray): Unit {
		if (isNodeJs) {
			require("crypto").randomFillSync(Uint8Array(data.unsafeCast<Array<Byte>>()))
		} else {
			global.crypto.getRandomValues(data)
		}
	}

	actual fun rootLocalVfs(): VfsFile = localVfs(".")
	actual fun applicationVfs(): VfsFile = localVfs(".")
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(".")
	actual fun userHomeVfs(): VfsFile = localVfs(".")
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)

	actual fun localVfs(path: String): VfsFile {
		return when {
			isNodeJs -> NodeJsLocalVfs(path).root
			else -> UrlVfs(path)
		}
	}

	val tmpdir: String by lazy {
		when {
			isNodeJs -> require("os").tmpdir().unsafeCast<String>()
			else -> "/tmp"
		}
	}


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
		val ji = KZlibInflater(nowrap)

		actual fun needsInput(): Boolean = ji.needsInput()
		actual fun setInput(buffer: ByteArray) = ji.setInput(buffer)
		actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = ji.inflate(buffer, offset, len)
		actual fun end() = ji.end()
	}

	actual object SyncCompression {
		val zlib by lazy { require("zlib") }

		actual fun inflate(data: ByteArray): ByteArray {
			when {
				OS.isNodejs -> {
					return zlib.inflateSync(data.toNodeJsBuffer()).unsafeCast<NodeJsBuffer>().toByteArray()
				}
				else -> {
					val out = ByteArrayOutputStream()
					val s = InflaterInputStream(ByteArrayInputStream(data))
					val temp = ByteArray(0x1000)
					while (true) {
						val read = s.read(temp, 0, temp.size)
						if (read <= 0) break
						out.write(temp, 0, read)
					}
					return out.toByteArray()
				}
			}
		}

		actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
			when {
				OS.isNodejs -> {
					val res = inflate(data)
					res.copyRangeTo(0, out, 0, min(res.size, out.size))
					return out
				}
				else -> {
					val s = InflaterInputStream(ByteArrayInputStream(data))
					var pos = 0
					var remaining = out.size
					while (true) {
						val read = s.read(out, pos, remaining)
						if (read <= 0) break
						pos += read
						remaining -= read
					}
					return out
				}
			}
		}

		actual fun deflate(data: ByteArray, level: Int): ByteArray {
			when {
				OS.isNodejs -> {
					return zlib.deflateSync(data.toNodeJsBuffer(), jsObject(
						"level" to level
					)).unsafeCast<NodeJsBuffer>().toByteArray()
				}
				else -> {
					val o = ByteArrayOutputStream()
					val out = DeflaterOutputStream(o, Deflater(level))
					out.write(data)
					return o.toByteArray()
				}
			}
		}
	}

	actual class SimplerMessageDigest actual constructor(name: String) {
		val nname = name.toLowerCase().replace("-", "")
		val hname: String = when (nname) {
			"hmacsha256", "sha256" -> "sha256"
			else -> invalidOp("Unsupported message digest '$name'")
		}

		val hash = require("crypto").createHash(hname)

		// @TODO: Optimize this!
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = update(data.copyOfRange(offset, offset + size))

		suspend fun update(data: ByteArray): Unit = hash.update(data)
		// @TODO: Optimize: Can return ByteArray directly?
		actual suspend fun digest(): ByteArray = Hex.decode(hash.digest("hex"))
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		val nname = name.toLowerCase().replace("-", "")
		val hname = when (nname) {
			"hmacsha256", "sha256" -> "sha256"
			"hmacsha1", "sha1" -> "sha1"
			else -> invalidOp("Unsupported hmac '$name'")
		}
		val hmac = require("crypto").createHmac(hname, key)
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit = update(data.copyOfRange(offset, offset + size))
		suspend fun update(data: ByteArray): Unit = hmac.update(data)
		actual suspend fun finalize(): ByteArray = Hex.decode(hmac.digest("hex"))
	}

	actual class NativeCRC32 {
		val crc = CRC32()
		actual fun update(data: ByteArray, offset: Int, size: Int): Unit = crc.update(data, offset, size)
		actual fun digest(): Int = crc.value
	}

	actual val httpFactory: HttpFactory by lazy {
		object : HttpFactory {
			override fun createClient(): HttpClient = if (OS.isNodejs) HttpClientNodeJs() else HttpClientBrowserJs()
			override fun createServer(): HttpServer = HttpSeverNodeJs()
		}
	}

	actual val ResourcesVfs: VfsFile by lazy { applicationVfs() }

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

	// Non-optimizable arrays
	actual fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)
	actual fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int) = KorioNativeDefaults.copyRangeTo(src, srcPos, dst, dstPos, count)

	// Using: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypedArray/set
	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) = dst.unsafeCast<Int8Array>().set(src.unsafeCast<Int8Array>().subarray(srcPos, srcPos + count), dstPos)
	actual fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) = dst.unsafeCast<Int16Array>().set(src.unsafeCast<Int16Array>().subarray(srcPos, srcPos + count), dstPos)
	actual fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) = dst.unsafeCast<Int32Array>().set(src.unsafeCast<Int32Array>().subarray(srcPos, srcPos + count), dstPos)
	actual fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) = dst.unsafeCast<Float32Array>().set(src.unsafeCast<Float32Array>().subarray(srcPos, srcPos + count), dstPos)
	actual fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) = dst.unsafeCast<Float64Array>().set(src.unsafeCast<Float64Array>().subarray(srcPos, srcPos + count), dstPos)

	// Non-optimizable arrays
	actual fun <T> fill(src: Array<T>, value: T, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)

	// @TODO: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/TypedArray/fill
	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)
	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) = KorioNativeDefaults.fill(src, value, from, to)

	val zlib by lazy { require("zlib") }

	suspend actual fun uncompressGzip(data: ByteArray): ByteArray = suspendCoroutine { c ->
		when {
			OS.isNodejs -> {
				zlib.gunzip(data.toNodeJsBuffer()) { error, data ->
					if (error != null) {
						c.resumeWithException(error)
					} else {
						c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
					}
				}
			}
			else -> {
				// Browser
				val out = ByteArrayOutputStream()
				GZIPInputStream(ByteArrayInputStream(data)).copyTo(out)
				c.resume(out.toByteArray())
			}
		}
	}

	suspend actual fun uncompressZlib(data: ByteArray): ByteArray = suspendCoroutine { c ->
		when {
			OS.isNodejs -> {
				zlib.inflate(data.toNodeJsBuffer()) { error, data ->
					if (error != null) {
						c.resumeWithException(error)
					} else {
						c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
					}
				}
			}
			else -> {
				val out = ByteArrayOutputStream()
				InflaterInputStream(ByteArrayInputStream(data)).copyTo(out)
				c.resume(out.toByteArray())
			}
		}
	}

	suspend actual fun compressGzip(data: ByteArray, level: Int): ByteArray = suspendCoroutine { c ->
		when {
			OS.isNodejs -> {
				zlib.gzip(data.toNodeJsBuffer()) { error, data ->
					if (error != null) {
						c.resumeWithException(error)
					} else {
						c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
					}
				}
			}
			else -> {
				val out = ByteArrayOutputStream()
				val out2 = GZIPOutputStream(out)
				ByteArrayInputStream(data).copyTo(out2)
				out2.flush()
				c.resume(out.toByteArray())
			}
		}
	}

	suspend actual fun compressZlib(data: ByteArray, level: Int): ByteArray = suspendCoroutine { c ->
		when {
			OS.isNodejs -> {
				zlib.deflate(data.toNodeJsBuffer()) { error, data ->
					if (error != null) {
						c.resumeWithException(error)
					} else {
						c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
					}
				}
			}
			else -> {
				val out = ByteArrayOutputStream()
				val deflater = Deflater(level)
				val out2 = DeflaterOutputStream(out, deflater)
				ByteArrayInputStream(data).copyTo(out2)
				out2.flush()
				c.resume(out.toByteArray())
			}
		}
	}

	actual class FastMemory(val buffer: Uint8Array, actual val size: Int) {
		val data = DataView(buffer.buffer)
		val i16 = Int16Array(buffer.buffer)
		val i32 = Int32Array(buffer.buffer)
		val f32 = Float32Array(buffer.buffer)

		companion actual object {
			actual fun alloc(size: Int): FastMemory = FastMemory(Uint8Array((size + 0xF) and 0xF.inv()), size)

			actual fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPos + n] = src[srcPos + n]
			}

			actual fun copy(src: FastMemory, srcPos: Int, dst: ByteArray, dstPos: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPos + n] = src[srcPos + n].toByte()
			}

			actual fun copy(src: ByteArray, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPos + n] = src[srcPos + n].toInt()
			}

			actual fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: ShortArray, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPosAligned + n] = src.getAlignedInt16(srcPosAligned + n)
			}

			actual fun copyAligned(src: ShortArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst.setAlignedInt16(dstPosAligned + n, src[srcPosAligned + n])
			}

			actual fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: IntArray, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPosAligned + n] = src.getAlignedInt32(srcPosAligned + n)
			}

			actual fun copyAligned(src: IntArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst.setAlignedInt32(dstPosAligned + n, src[srcPosAligned + n])
			}

			actual fun copyAligned(src: FastMemory, srcPosAligned: Int, dst: FloatArray, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst[dstPosAligned + n] = src.getAlignedFloat32(srcPosAligned + n)
			}

			actual fun copyAligned(src: FloatArray, srcPosAligned: Int, dst: FastMemory, dstPosAligned: Int, length: Int): Unit {
				// @TODO: Optimize
				for (n in 0 until length) dst.setAlignedFloat32(dstPosAligned + n, src[srcPosAligned + n])
			}
		}

		actual operator fun get(index: Int): Int = buffer[index].toInt() and 0xFF
		actual operator fun set(index: Int, value: Int): Unit = run { buffer[index] = value.toByte() }

		actual fun setAlignedInt16(index: Int, value: Short) = run { i16[index] = value }
		actual fun getAlignedInt16(index: Int): Short = i16[index]
		actual fun setAlignedInt32(index: Int, value: Int) = run { i32[index] = value }
		actual fun getAlignedInt32(index: Int): Int = i32[index]
		actual fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32[index] = value }
		actual fun getAlignedFloat32(index: Int): Float = f32[index]

		actual fun setInt16(index: Int, value: Short): Unit = run { data.setInt16(index, value) }
		actual fun getInt16(index: Int): Short = data.getInt16(index)
		actual fun setInt32(index: Int, value: Int): Unit = run { data.setInt32(index, value) }
		actual fun getInt32(index: Int): Int = data.getInt32(index)
		actual fun setFloat32(index: Int, value: Float): Unit = run { data.setFloat32(index, value) }
		actual fun getFloat32(index: Int): Float = data.getFloat32(index)

		actual fun setArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
		actual fun setAlignedArrayInt8(dstPos: Int, src: ByteArray, srcPos: Int, len: Int) = copy(src, srcPos, this, dstPos, len)
		actual fun setAlignedArrayInt16(dstPos: Int, src: ShortArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)
		actual fun setAlignedArrayInt32(dstPos: Int, src: IntArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)
		actual fun setAlignedArrayFloat32(dstPos: Int, src: FloatArray, srcPos: Int, len: Int) = copyAligned(src, srcPos, this, dstPos, len)

		actual fun getArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
		actual fun getAlignedArrayInt8(srcPos: Int, dst: ByteArray, dstPos: Int, len: Int) = copy(this, srcPos, dst, dstPos, len)
		actual fun getAlignedArrayInt16(srcPos: Int, dst: ShortArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
		actual fun getAlignedArrayInt32(srcPos: Int, dst: IntArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
		actual fun getAlignedArrayFloat32(srcPos: Int, dst: FloatArray, dstPos: Int, len: Int) = copyAligned(this, srcPos, dst, dstPos, len)
	}

	actual fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		global.testPromise = kotlin.js.Promise<Unit> { resolve, reject ->
			val el = EventLoopTest()
			var done = false
			spawnAndForget(el.coroutineContext) {
				try {
					block(el)
					resolve(Unit)
				} catch (e: Throwable) {
					reject(e)
				} finally {
					done = true
				}
			}
			fun step() {
				global.setTimeout({
					el.step(10)
					if (!done) step()
				}, 0)
			}
			step()
		}
	}
}

external private class Date(time: Double)


private class EventLoopFactoryJs : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopJs()
}

@Suppress("unused")
private class EventLoopJs : EventLoop(captureCloseables = false) {
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
		val id = global.requestAnimationFrame(callback)
		//println("setTimeout($ms)")
		return Closeable { global.cancelAnimationFrame(id) }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		//println("setInterval($ms)")
		val id = global.setInterval({ callback() }, ms)
		return Closeable { global.clearInterval(id) }
	}
}

//@JsName("require")

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
fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
	val out = jsEmptyObj()
	for (pair in pairs) out[pair.first] = pair.second
	return out
}

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

class CRC32 {
	/*
	 *  The following logic has come from RFC1952.
     */
	private var v = 0

	val value: Int get() = v

	fun update(buf: ByteArray, index: Int, len: Int) {
		var index = index
		var len = len
		//int[] crc_table = CRC32.crc_table;
		var c = v.inv()
		while (--len >= 0) {
			c = crc_table!![c xor buf[index++].toInt() and 0xff] xor c.ushr(8)
		}
		v = c.inv()
	}

	fun reset() {
		v = 0
	}

	fun reset(vv: Int) {
		v = vv
	}

	fun copy(): CRC32 {
		val foo = CRC32()
		foo.v = this.v
		return foo
	}

	companion object {
		private var crc_table: IntArray = IntArray(256)

		init {
			for (n in 0..255) {
				var c = n
				var k = 8
				while (--k >= 0) {
					if (c and 1 != 0) {
						c = -0x12477ce0 xor c.ushr(1)
					} else {
						c = c.ushr(1)
					}
				}
				crc_table[n] = c
			}
		}

		// The following logic has come from zlib.1.2.
		private val GF2_DIM = 32

		internal fun combine(crc1: Long, crc2: Long, len2: Long): Long {
			var crc1 = crc1
			var len2 = len2
			var row: Long
			val even = LongArray(GF2_DIM)
			val odd = LongArray(GF2_DIM)

			// degenerate case (also disallow negative lengths)
			if (len2 <= 0) return crc1

			// put operator for one zero bit in odd
			odd[0] = 0xedb88320L          // CRC-32 polynomial
			row = 1
			for (n in 1 until GF2_DIM) {
				odd[n] = row
				row = row shl 1
			}

			// put operator for two zero bits in even
			gf2_matrix_square(even, odd)

			// put operator for four zero bits in odd
			gf2_matrix_square(odd, even)

			// apply len2 zeros to crc1 (first square will put the operator for one
			// zero byte, eight zero bits, in even)
			do {
				// apply zeros operator for this bit of len2
				gf2_matrix_square(even, odd)
				if (len2 and 1 != 0L) crc1 = gf2_matrix_times(even, crc1)
				len2 = len2 shr 1

				// if no more bits set, then done
				if (len2 == 0L) break

				// another iteration of the loop with odd and even swapped
				gf2_matrix_square(odd, even)
				if (len2 and 1 != 0L) crc1 = gf2_matrix_times(odd, crc1)
				len2 = len2 shr 1

				// if no more bits set, then done
			} while (len2 != 0L)

			/* return combined crc */
			crc1 = crc1 xor crc2
			return crc1
		}

		private fun gf2_matrix_times(mat: LongArray, vec: Long): Long {
			var vec = vec
			var sum: Long = 0
			var index = 0
			while (vec != 0L) {
				if (vec and 1 != 0L)
					sum = sum xor mat[index]
				vec = vec shr 1
				index++
			}
			return sum
		}

		internal fun gf2_matrix_square(square: LongArray, mat: LongArray) {
			for (n in 0 until GF2_DIM)
				square[n] = gf2_matrix_times(mat, mat[n])
		}

		val crC32Table: IntArray
			get() {
				val tmp = IntArray(crc_table.size)
				crc_table.copyRangeTo(0, tmp, 0, tmp.size)
				return tmp
			}
	}
}

class KZlibInflater(private var nowrap: Boolean = false) {
	private var inf: Inflater? = Inflater(nowrap)
	private var needDict: Boolean = false
	var bytesRead: Long = 0; private set
	var bytesWritten: Long = 0; private set

	//System.out.println("getRemaining()=" + inf.getAvailIn());
	val remaining: Int get() = inf!!.avail_in
	val adler: Int get() = inf!!.getAdler()
	val totalIn: Int get() = bytesRead.toInt()
	val totalOut: Int get() = bytesWritten.toInt()
	fun setInput(b: ByteArray, off: Int = 0, len: Int = b.size) = run { inf!!.setInput(b, off, len, true) }
	fun setDictionary(b: ByteArray, off: Int = 0, len: Int = b.size) = run { inf!!.setDictionary(b, off, len); needDict = false }
	fun needsInput(): Boolean = remaining <= 0
	fun needsDictionary(): Boolean = needDict
	fun finished(): Boolean = inf!!.finished()

	fun inflate(b: ByteArray, off: Int = 0, len: Int = b.size): Int {
		val instart = inf!!.total_in
		inf!!.setOutput(b, off, len)

		val outstart = inf!!.total_out
		//inf.inflate(len);
		val err = inf!!.inflate(JZlib.Z_NO_FLUSH)
		val outend = inf!!.total_out
		val inend = inf!!.total_in

		//System.out.println("inflate: " + instart + "/" + inend + " || " + outstart + "/" + outend);

		val n = (outend - outstart).toInt()
		bytesWritten += n.toLong()
		bytesRead += (inend - instart).toInt().toLong()
		return n
	}

	fun reset() {
		inf!!.free()
		inf!!.init(nowrap)
		needDict = true
		bytesRead = 0
		bytesWritten = 0
	}

	fun end() {
		//inf.inflateEnd()
		inf!!.end()
		needDict = true
		bytesRead = 0
		bytesWritten = 0
	}
}
