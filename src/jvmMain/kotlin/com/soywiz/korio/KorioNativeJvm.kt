package com.soywiz.korio

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.net.ws.*
import kotlinx.coroutines.*
import java.io.*
import java.security.*
import java.util.*
import kotlin.coroutines.*

actual object KorioNative {
	internal val workerContext by lazy { newSingleThreadContext("worker") }

	private val secureRandom: SecureRandom by lazy { SecureRandom.getInstanceStrong() }

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
