package com.soywiz.korio

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.crypto.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import org.khronos.webgl.*
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.xhr.*
import kotlin.browser.*
import kotlin.collections.set
import kotlin.coroutines.*
import kotlin.reflect.*

/*
actual annotation class Language actual constructor(
	actual val value: String,
	actual val prefix: String = "",
	actual val suffix: String = ""
)
*/

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual open class RuntimeException actual constructor(msg: String) : Exception(msg)
actual open class IllegalStateException actual constructor(msg: String) : RuntimeException(msg)

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

	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.simpleName ?: "unknown"

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T {
		// @TODO: Use JS worker
		return callback()
	}

	actual fun asyncEntryPoint(context: CoroutineContext, callback: suspend () -> Unit): dynamic {
		//callback.startCoroutine(EmptyContinuation(context))
		return kotlin.js.Promise<dynamic> { resolve, reject ->
			callback.startCoroutine(object : Continuation<Unit> {
				override val context: CoroutineContext = context

				override fun resumeWith(result: Result<Unit>) {
					val exception = result.exceptionOrNull()
					if (exception != null) {
						reject(exception)
					} else {
						//resolve(undefined)
						resolve(Unit)
					}
				}
			})
		}
	}

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

	private val absoluteCwd: String = if (isNodeJs) require("path").resolve(".") else "."

	actual fun rootLocalVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun applicationVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun applicationDataVfs(): VfsFile = jsLocalStorageVfs.root
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun userHomeVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)

	actual fun localVfs(path: String): VfsFile {
		return when {
			isNodeJs -> NodeJsLocalVfs()[path]
			else -> UrlVfs(document.location?.href ?: ".")[path]
		}
	}

	val tmpdir: String by lazy {
		when {
			isNodeJs -> require("os").tmpdir().unsafeCast<String>()
			else -> "/tmp"
		}
	}

	actual fun uncompress(input: ByteArray, outputHint: Int, method: String): ByteArray {
		return FastDeflate.uncompress(input, outputHint, method)
	}

	actual fun compress(input: ByteArray, outputHint: Int, method: String, level: Int): ByteArray {
		return input.syncCompress(ZLib, CompressionContext(level = level))
	}

	actual val File_separatorChar: Char = '/'

	actual val asyncSocketFactory: AsyncSocketFactory by lazy {
		object : AsyncSocketFactory() {
			override suspend fun createClient(secure: Boolean): AsyncClient = NodeJsAsyncClient()
			override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
				NodeJsAsyncServer().init(port, host, backlog)
		}
	}

	actual val websockets: WebSocketClientFactory by lazy { JsWebSocketClientFactory() }

	// @NOTE: Important to keep return value or it will believe that this is a List<dynamic>
	actual val systemLanguageStrings: List<String> by lazy {
		if (isNodeJs) {
			val env = process.env
			listOf<String>(env.LANG ?: env.LANGUAGE ?: env.LC_ALL ?: env.LC_MESSAGES ?: "english")
		} else {
			//console.log("window.navigator.languages", window.navigator.languages)
			//console.log("window.navigator.languages", window.navigator.languages.toList())
			window.navigator.languages.toList()
			//val langs = window.navigator.languages
			//(0 until langs.size).map { "" + langs[it] + "-" }
		}
	}

	actual fun Thread_sleep(time: Long) {}

	actual class SimplerMessageDigest actual constructor(name: String) {
		val nname = name.toLowerCase().replace("-", "")
		val hname: String = when (nname) {
			"hmacsha256", "sha256" -> "sha256"
			else -> invalidOp("Unsupported message digest '$name'")
		}

		val hash = require("crypto").createHash(hname)

		// @TODO: Optimize this!
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit =
			update(data.copyOfRange(offset, offset + size))

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
		actual suspend fun update(data: ByteArray, offset: Int, size: Int): Unit =
			update(data.copyOfRange(offset, offset + size))

		suspend fun update(data: ByteArray): Unit = hmac.update(data)
		actual suspend fun finalize(): ByteArray = Hex.decode(hmac.digest("hex"))
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

	actual fun getenv(key: String): String? {
		if (OS.isNodejs) {
			return process.env[key]
		} else {
			val qs = QueryString.decode((document.location?.search ?: "").trimStart('?'))
			val envs = qs.map { it.key.toUpperCase() to (it.value.firstOrNull() ?: it.key) }.toMap()
			return envs[key.toUpperCase()]
		}
	}

	//actual fun suspendTest(callback: suspend () -> Unit): dynamic = promise { callback() }
	actual fun suspendTest(callback: suspend () -> Unit): dynamic {
		return kotlin.js.Promise<dynamic> { resolve, reject ->
			callback.startCoroutine(object : Continuation<Unit> {
				override val context: CoroutineContext = KorioDefaultDispatcher
				override fun resumeWith(result: Result<Unit>) {
					val exception = result.exceptionOrNull()
					if (exception != null) {
						reject(exception)
					} else {
						//resolve(undefined)
						resolve(Unit)
					}
				}
			})
		}
	}
}

private external class Date(time: Double)

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
fun jsArray(vararg elements: dynamic): Array<dynamic> {
	val out = jsEmptyArray()
	for (e in elements) out.push(e)
	return out
}

inline fun <reified T> jsToArrayT(obj: dynamic): Array<T> = Array<T>(obj.length) { obj[it] }
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
	override suspend fun requestInternal(
		method: Http.Method,
		url: String,
		headers: Http.Headers,
		content: AsyncStream?
	): Response {
		val deferred = CompletableDeferred<Response>(Job())
		val xhr = XMLHttpRequest()
		xhr.open(method.name, url, true)
		xhr.responseType = XMLHttpRequestResponseType.ARRAYBUFFER

		xhr.onload = { e ->
			val u8array = Uint8Array(xhr.response as ArrayBuffer)
			val out = ByteArray(u8array.length)
			for (n in out.indices) out[n] = u8array[n]
			//js("debugger;")
			deferred.complete(
				Response(
					status = xhr.status.toInt(),
					statusText = xhr.statusText,
					headers = Http.Headers(xhr.getAllResponseHeaders()),
					content = out.openAsync()
				)
			)
		}

		xhr.onerror = { e ->
			deferred.completeExceptionally(kotlin.RuntimeException("Error ${xhr.status} opening $url"))
		}

		for (header in headers) {
			val hnname = header.first.toLowerCase().trim()
			when (hnname) {
				"connection", "content-length" -> Unit // Refused to set unsafe header
				else -> xhr.setRequestHeader(header.first, header.second)
			}
		}

		deferred.invokeOnCompletion {
			if (deferred.isCancelled) {
				xhr.abort()
			}
		}

		if (content != null) {
			xhr.send(content.readAll())
		} else {
			xhr.send()
		}
		return deferred.await()
	}
}


class JsWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(
		url: String,
		protocols: List<String>?,
		origin: String?,
		wskey: String?,
		debug: Boolean
	): WebSocketClient = JsWebSocketClient(url, protocols, DEBUG = debug).apply { init() }
}

class JsWebSocketClient(url: String, protocols: List<String>?, val DEBUG: Boolean) :
	WebSocketClient(url, protocols, true) {
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
