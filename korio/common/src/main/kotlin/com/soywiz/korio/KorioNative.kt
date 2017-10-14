package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.lang.Console
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toByteArray
import com.soywiz.korio.lang.toString
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.ws.WebSocketClientFactory
import com.soywiz.korio.stream.readUntil
import com.soywiz.korio.vfs.LocalVfsProvider
import com.soywiz.korio.vfs.VfsFile
import kotlin.reflect.KClass

expect annotation class Synchronized
expect annotation class JvmField
expect annotation class JvmStatic
expect annotation class JvmOverloads
expect annotation class Transient

expect open class IOException(msg: String) : Exception(msg)
expect open class EOFException(msg: String) : IOException(msg)
expect open class FileNotFoundException(msg: String) : IOException(msg)

expect open class RuntimeException(msg: String) : Exception(msg)
expect open class IllegalStateException(msg: String) : RuntimeException(msg)
expect open class CancellationException(msg: String) : IllegalStateException(msg)

expect object KorioNative {
	abstract class NativeThreadLocal<T> {
		abstract fun initialValue(): T
		fun get(): T
		fun set(value: T): Unit
	}

	val currentThreadId: Long
	val platformName: String
	val osName: String
	val ResourcesVfs: VfsFile

	val websockets: WebSocketClientFactory

	fun currentTimeMillis(): Long

	// In minutes to add
	fun getLocalTimezoneOffset(time: Long): Int

	val eventLoopFactoryDefaultImpl: EventLoopFactory

	suspend fun <T> executeInWorker(callback: suspend () -> T): T

	val File_separatorChar: Char

	val localVfsProvider: LocalVfsProvider
	val tmpdir: String

	fun Thread_sleep(time: Long): Unit

	val asyncSocketFactory: AsyncSocketFactory

	val httpFactory: HttpFactory

	fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int)
	fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int)
	fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int)
	fun <T> fill(src: Array<T>, value: T, from: Int, to: Int)
	fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int)
	fun fill(src: ByteArray, value: Byte, from: Int, to: Int)
	fun fill(src: ShortArray, value: Short, from: Int, to: Int)
	fun fill(src: IntArray, value: Int, from: Int, to: Int)
	fun fill(src: FloatArray, value: Float, from: Int, to: Int)
	fun fill(src: DoubleArray, value: Double, from: Int, to: Int)

	fun printStackTrace(e: Throwable)
	fun enterDebugger()
	fun log(msg: Any?)
	fun error(msg: Any?)

	suspend fun uncompressGzip(data: ByteArray): ByteArray
	suspend fun uncompressZlib(data: ByteArray): ByteArray
	suspend fun compressGzip(data: ByteArray, level: Int): ByteArray
	suspend fun compressZlib(data: ByteArray, level: Int): ByteArray

	class SimplerMessageDigest(name: String) {
		suspend fun update(data: ByteArray, offset: Int, size: Int): Unit
		suspend fun digest(): ByteArray
	}

	class SimplerMac(name: String, key: ByteArray) {
		suspend fun update(data: ByteArray, offset: Int, size: Int)
		suspend fun finalize(): ByteArray
	}

	class NativeCRC32 {
		fun update(data: ByteArray, offset: Int, size: Int)
		fun digest(): Int
	}

	object CreateAnnotation {
		fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T
	}

	class Inflater(nowrap: Boolean) {
		fun needsInput(): Boolean
		fun setInput(buffer: ByteArray): Unit
		fun inflate(buffer: ByteArray, offset: Int, len: Int): Int
		fun end(): Unit
	}

	object SyncCompression {
		fun inflate(data: ByteArray): ByteArray
		fun inflateTo(data: ByteArray, out: ByteArray): ByteArray
		fun deflate(data: ByteArray, level: Int): ByteArray
	}

	class FastMemory {
		companion object {
			fun alloc(size: Int): FastMemory
			fun copy(src: FastMemory, srcPos: Int, dst: FastMemory, dstPos: Int, length: Int): Unit
		}

		val size: Int

		operator fun get(index: Int): Int
		operator fun set(index: Int, value: Int): Unit

		fun setAlignedInt16(index: Int, value: Short): Unit
		fun getAlignedInt16(index: Int): Short

		fun setAlignedInt32(index: Int, value: Int): Unit
		fun getAlignedInt32(index: Int): Int

		fun setAlignedFloat32(index: Int, value: Float): Unit
		fun getAlignedFloat32(index: Int): Float

		fun setAlignedArrayInt8(index: Int, data: ByteArray, offset: Int, len: Int)
		fun setAlignedArrayInt16(index: Int, data: ShortArray, offset: Int, len: Int)
		fun setAlignedArrayInt32(index: Int, data: IntArray, offset: Int, len: Int)
		fun setAlignedArrayFloat32(index: Int, data: FloatArray, offset: Int, len: Int)

		fun getInt16(index: Int): Short
		fun getInt32(index: Int): Int
		fun getFloat32(index: Int): Float
	}
}

object KorioNativeDefaults {
	private inline fun overlaps(src: Any, srcPos: Int, dst: Any, dstPos: Int, count: Int): Boolean {
		return (src === dst) && srcPos >= dstPos
	}

	fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) {
		if (overlaps(src, srcPos, dst, dstPos, count)) {
			for (n in 0 until count) dst[dstPos + n] = src[srcPos + n]
		} else {
			for (n in count - 1 downTo 0) dst[dstPos + n] = src[srcPos + n]
		}
	}

	fun <T> fill(src: Array<T>, value: T, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: ByteArray, value: Byte, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: ShortArray, value: Short, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: IntArray, value: Int, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: FloatArray, value: Float, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun fill(src: DoubleArray, value: Double, from: Int, to: Int) {
		for (n in from until to) src[n] = value
	}

	fun printStackTrace(e: Throwable) {
		Console.error("KorioNativeDefaults.printStackTrace:")
		Console.error(e.message ?: "Error")
	}

	fun createServer(): HttpServer {
		val HeaderRegex = Regex("^(\\w+)\\s+(.*)\\s+HTTP/1.\\d$")

		return object : HttpServer() {
			var wshandler: suspend (WsRequest) -> Unit = {}
			var handler: suspend (Request) -> Unit = {}
			val onClose = Signal<Unit>()
			override var actualPort: Int = -1; private set

			suspend override fun websocketHandlerInternal(handler: suspend (WsRequest) -> Unit) {
				this.wshandler = handler
			}

			suspend override fun httpHandlerInternal(handler: suspend (Request) -> Unit) {
				this.handler = handler
			}

			suspend override fun listenInternal(port: Int, host: String) {
				val socket = KorioNative.asyncSocketFactory.createServer(port, host)
				actualPort = socket.port
				tasksInProgress.incrementAndGet()
				val close = socket.listen { client ->
					while (true) {
						println("Connected! : $client : ${KorioNative.currentThreadId}")
						//val cb = client.toBuffered()
						val cb = client

						//val header = cb.readBufferedLine().trim()
						//val fline = cb.readBufferedUntil('\n'.toByte()).toString(UTF8).trim()
						val fline = cb.readUntil('\n'.toByte()).toString(UTF8).trim()
						println("fline: $fline")
						val match = HeaderRegex.matchEntire(fline) ?: throw IllegalStateException("Not a valid request '$fline'")
						val method = match.groupValues[1]
						val url = match.groupValues[2]
						val headerList = arrayListOf<Pair<String, String>>()
						for (n in 0 until 1024) { // up to 1024 headers
							val line = cb.readUntil('\n'.toByte()).toString(UTF8).trim()
							if (line.isEmpty()) break
							val parts = line.split(':', limit = 2)
							headerList += parts[0] to parts.getOrElse(1) { "" }
						}
						val headers = Http.Headers(headerList)
						val keepAlive = headers["connection"]?.toLowerCase() == "keep-alive"

						//println("REQ: $method, $url, $headerList")

						val requestCompleted = Promise.Deferred<Unit>()

						spawnAndForget {
							handler(object : HttpServer.Request(Http.Method(method), url, headers) {
								suspend override fun _handler(handler: (ByteArray) -> Unit) {
									TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
								}

								suspend override fun _endHandler(handler: () -> Unit) {
									TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
								}

								override suspend fun _sendHeader(code: Int, message: String, headers: Http.Headers) {
									val sb = StringBuilder()
									sb.append("HTTP/1.1 $code $message\r\n")
									for (header in headers) sb.append("${header.first}: ${header.second}\r\n")
									sb.append("\r\n")
									client.write(sb.toString().toByteArray(UTF8))
								}

								override suspend fun _write(data: ByteArray, offset: Int, size: Int) {
									client.write(data, offset, size)
								}

								override suspend fun _end() {
									requestCompleted.resolve(Unit)
								}
							})
						}

						requestCompleted.promise.await()

						//if (keepAlive) continue

						client.close()
						break
					}
				}

				onClose {
					close.close()
					tasksInProgress.decrementAndGet()
				}
			}

			suspend override fun closeInternal() {
				onClose()
			}
		}
	}
}
