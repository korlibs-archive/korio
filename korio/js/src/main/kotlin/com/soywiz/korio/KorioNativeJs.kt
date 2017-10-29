package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.async.Promise
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
import com.soywiz.korio.stream.*
import com.soywiz.korio.typedarray.copyRangeTo
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.*
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.xhr.ARRAYBUFFER
import org.w3c.xhr.XMLHttpRequest
import org.w3c.xhr.XMLHttpRequestResponseType
import kotlin.browser.window
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.js.*
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

	actual val tmpdir: String by lazy {
		when {
			isNodeJs -> require("os").tmpdir()
			else -> "/tmp"
		}

	}

	actual val localVfsProvider: LocalVfsProvider by lazy {
		object : LocalVfsProvider() {
			//override fun invoke(): LocalVfs = UrlVfs(".").vfs
			override fun invoke(): LocalVfs = NodeJsLocalVfs(".")
			override fun invoke(path: String): VfsFile = NodeJsLocalVfs(path).root

			override fun getCacheFolder(): String = "cache"
			override fun getExternalStorageFolder(): String = "."
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
		actual fun needsInput(): Boolean = TODO()
		actual fun setInput(buffer: ByteArray): Unit = TODO()
		actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = TODO()
		actual fun end(): Unit = TODO()
	}

	actual object SyncCompression {
		val zlib = require("zlib")

		actual fun inflate(data: ByteArray): ByteArray {
			return zlib.inflateSync(data.toNodeJsBuffer()).unsafeCast<NodeJsBuffer>().toByteArray()
		}

		actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
			val res = inflate(data)
			res.copyRangeTo(0, out, 0, min(res.size, out.size))
			return out
		}

		actual fun deflate(data: ByteArray, level: Int): ByteArray {
			return zlib.deflateSync(data.toNodeJsBuffer(), jsObject(
				"level" to level
			)).unsafeCast<NodeJsBuffer>().toByteArray()
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

	actual val ResourcesVfs: VfsFile by lazy {
		when {
			isNodeJs -> localVfsProvider.invoke(".")
			else -> UrlVfs(".")
		}
	}

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

	val zlib by lazy { require("zlib") }

	suspend actual fun uncompressGzip(data: ByteArray): ByteArray = suspendCoroutine { c ->
		zlib.gunzip(data.toNodeJsBuffer()) { error, data ->
			if (error != null) {
				c.resumeWithException(error)
			} else {
				c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
			}
		}
	}

	suspend actual fun uncompressZlib(data: ByteArray): ByteArray = suspendCoroutine { c ->
		zlib.inflate(data.toNodeJsBuffer()) { error, data ->
			if (error != null) {
				c.resumeWithException(error)
			} else {
				c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
			}
		}
	}

	suspend actual fun compressGzip(data: ByteArray, level: Int): ByteArray = suspendCoroutine { c ->
		zlib.gzip(data.toNodeJsBuffer()) { error, data ->
			if (error != null) {
				c.resumeWithException(error)
			} else {
				c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
			}
		}
	}

	suspend actual fun compressZlib(data: ByteArray, level: Int): ByteArray = suspendCoroutine { c ->
		zlib.deflate(data.toNodeJsBuffer()) { error, data ->
			if (error != null) {
				c.resumeWithException(error)
			} else {
				c.resume(data.unsafeCast<NodeJsBuffer>().toByteArray())
			}
		}
	}

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
