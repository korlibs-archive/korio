package com.soywiz.korio

import com.soywiz.korio.compression.*
import com.soywiz.korio.compression.deflate.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import kotlinx.coroutines.*
import java.io.*
import java.security.*
import java.util.*
import javax.crypto.*
import javax.crypto.spec.*
import kotlin.coroutines.*
import kotlin.reflect.*

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

val currentThreadId: Long get() = KorioNative.currentThreadId

actual object KorioNative {
	actual val currentThreadId: Long get() = Thread.currentThread().id

	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.java.simpleName

	internal val workerContext by lazy { newSingleThreadContext("worker") }

	actual fun asyncEntryPoint(context: CoroutineContext, callback: suspend () -> Unit) =
		runBlocking(context) { callback() }

	actual val systemLanguageStrings get() = listOf(Locale.getDefault().isO3Language)

	actual val platformName: String = "jvm"
	actual val rawOsName: String by lazy { System.getProperty("os.name") }

	private val secureRandom: SecureRandom by lazy { SecureRandom.getInstanceStrong() }

	actual fun getRandomValues(data: ByteArray): Unit {
		secureRandom.nextBytes(data)
	}

	actual val httpFactory: HttpFactory by lazy {
		object : HttpFactory {
			init {
				System.setProperty("http.keepAlive", "false")
			}

			override fun createClient(): HttpClient = HttpClientJvm()
			override fun createServer(): HttpServer = KorioNativeDefaults.createServer()
		}
	}

	actual fun Thread_sleep(time: Long) = Thread.sleep(time)

	actual val websockets: WebSocketClientFactory get() = com.soywiz.korio.net.ws.RawSocketWebSocketClientFactory
	actual val File_separatorChar: Char by lazy { File.separatorChar }

	actual fun uncompress(input: ByteArray, outputHint: Int, method: String): ByteArray {
		return FastDeflate.uncompress(input, outputHint, method)
	}

	actual fun compress(input: ByteArray, outputHint: Int, method: String, level: Int): ByteArray {
		return input.syncCompress(ZLib, CompressionContext(level = level))
	}

	private val absoluteCwd = File(".").absolutePath

	actual fun rootLocalVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun applicationVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun applicationDataVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun userHomeVfs(): VfsFile = localVfs(absoluteCwd)
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)
	actual fun localVfs(path: String): VfsFile = LocalVfsJvm()[path]

	actual val ResourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root.jail() }

	val tmpdir: String get() = System.getProperty("java.io.tmpdir")

	actual fun enterDebugger() = Unit
	actual fun printStackTrace(e: Throwable) = e.printStackTrace()

	actual fun getenv(key: String): String? {
		return System.getenv(key)
	}

	actual fun suspendTest(callback: suspend () -> Unit): Unit {
		runBlocking { callback() }
	}
}

/*
class JvmWebSocketClientFactory : WebSocketClientFactory() {
	override suspend fun create(
		url: String,
		protocols: List<String>?,
		origin: String?,
		wskey: String?,
		debug: Boolean
	): WebSocketClient {
		return object : WebSocketClient(url, protocols, false) {
			val that = this

			val client = object : org.java_websocket.client.WebSocketClient(java.net.URI(url)) {
				override fun onOpen(handshakedata: ServerHandshake) {
					that.onOpen(Unit)
				}

				override fun onClose(code: Int, reason: String, remote: Boolean) {
					that.onClose(Unit)
				}

				override fun onMessage(message: String) {
					that.onStringMessage(message)
					that.onAnyMessage(message)
				}

				override fun onMessage(bytes: ByteBuffer) {
					val rbytes = bytes.toByteArray()
					that.onBinaryMessage(rbytes)
					that.onAnyMessage(rbytes)
				}

				override fun onError(ex: Exception) {
					that.onError(ex)
				}
			}

			suspend fun init() {
				client.connect()
			}

			override fun close(code: Int, reason: String) {
				client.close(code, reason)
			}

			override suspend fun send(message: String) {
				client.send(message)
			}

			override suspend fun send(message: ByteArray) {
				client.send(message)
			}
		}.apply {
			init()
			val res = listOf(onOpen, onError).waitOne()
			if (res is Throwable) throw res
		}
	}
}
*/

suspend fun <T> executeInWorkerJVM(callback: suspend () -> T): T = withContext(KorioNative.workerContext) { callback() }
