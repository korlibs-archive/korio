package com.soywiz.korio

import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.eventLoop
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.JvmAsyncSocketFactory
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpClientJvm
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.ws.WebSocketClientFactory
import com.soywiz.korio.vfs.LocalVfsJvm
import com.soywiz.korio.vfs.MemoryVfs
import com.soywiz.korio.vfs.ResourcesVfsProviderJvm
import com.soywiz.korio.vfs.VfsFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KClass


actual typealias Synchronized = kotlin.jvm.Synchronized
actual typealias JvmField = kotlin.jvm.JvmField
actual typealias JvmStatic = kotlin.jvm.JvmStatic
actual typealias JvmOverloads = kotlin.jvm.JvmOverloads
actual typealias Transient = kotlin.jvm.Transient

actual typealias Language = org.intellij.lang.annotations.Language

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

actual typealias RuntimeException = java.lang.RuntimeException
actual typealias IllegalStateException = java.lang.IllegalStateException
actual typealias CancellationException = java.util.concurrent.CancellationException

actual class Semaphore actual constructor(initial: Int) {
	val jsema = java.util.concurrent.Semaphore(initial)
	//var initial: Int
	actual fun acquire() = jsema.acquire()

	actual fun release() = jsema.release()
}

actual object KorioNative {
	actual val currentThreadId: Long get() = Thread.currentThread().id

	actual fun getClassSimpleName(clazz: KClass<*>): String = clazz.java.name

	actual abstract class NativeThreadLocal<T> {
		actual abstract fun initialValue(): T

		val jthreadLocal = object : ThreadLocal<T>() {
			override fun initialValue(): T = this@NativeThreadLocal.initialValue()
		}

		actual fun get(): T = jthreadLocal.get()
		actual fun set(value: T) = jthreadLocal.set(value)
	}

	suspend private fun <T> _executeInside(task: suspend () -> T, executionScope: (body: () -> Unit) -> Unit): T {
		val deferred = Promise.Deferred<T>()
		val parentEventLoop = eventLoop()
		tasksInProgress.incrementAndGet()
		executionScope {
			syncTest {
				try {
					val res = task()
					parentEventLoop.queue {
						deferred.resolve(res)
					}
				} catch (e: Throwable) {
					parentEventLoop.queue { deferred.reject(e) }
				} finally {
					tasksInProgress.decrementAndGet()
				}
			}
		}
		return deferred.promise.await()
	}

	actual suspend fun <T> executeInNewThread(callback: suspend () -> T): T = _executeInside(callback) { body ->
		Thread {
			body()
		}.apply {
			isDaemon = true
			start()
		}
	}

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T = _executeInside(callback) { body ->
		workerLazyPool.executeUpdatingTasksInProgress {
			body()
		}
	}

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

	actual class NativeCRC32 {
		val crc32 = java.util.zip.CRC32()

		actual fun update(data: ByteArray, offset: Int, size: Int) {
			crc32.update(data, offset, size)
		}

		actual fun digest(): Int {
			return crc32.value.toInt()
		}
	}

	actual class SimplerMessageDigest actual constructor(name: String) {
		val md = MessageDigest.getInstance(name)

		actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorker { md.update(data, offset, size) }
		actual suspend fun digest(): ByteArray = executeInWorker { md.digest() }
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		val mac = Mac.getInstance(name).apply { init(SecretKeySpec(key, name)) }
		actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorker { mac.update(data, offset, size) }
		actual suspend fun finalize(): ByteArray = executeInWorker { mac.doFinal() }
	}

	actual object SyncCompression {
		actual fun inflate(data: ByteArray): ByteArray {
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

		actual fun inflateTo(data: ByteArray, out: ByteArray): ByteArray {
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

		actual fun deflate(data: ByteArray, level: Int): ByteArray {
			return DeflaterInputStream(ByteArrayInputStream(data), Deflater(level)).readBytes()
		}
	}

	actual class Inflater actual constructor(val nowrap: Boolean) {
		val ji = java.util.zip.Inflater(nowrap)

		actual fun needsInput(): Boolean = ji.needsInput()
		actual fun setInput(buffer: ByteArray) = ji.setInput(buffer)
		actual fun inflate(buffer: ByteArray, offset: Int, len: Int): Int = ji.inflate(buffer, offset, len)
		actual fun end() = ji.end()
	}

	actual fun Thread_sleep(time: Long) = Thread.sleep(time)

	actual val eventLoopFactoryDefaultImpl: EventLoopFactory = EventLoopFactoryJvmAndCSharp()

	actual val asyncSocketFactory: AsyncSocketFactory by lazy { JvmAsyncSocketFactory() }
	actual val websockets: WebSocketClientFactory get() = TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	actual val File_separatorChar: Char by lazy { File.separatorChar }

	actual fun rootLocalVfs(): VfsFile = localVfs(".")
	actual fun applicationVfs(): VfsFile = localVfs(File(".").absolutePath)
	actual fun applicationDataVfs(): VfsFile = localVfs(File(".").absolutePath)
	actual fun cacheVfs(): VfsFile = MemoryVfs()
	actual fun externalStorageVfs(): VfsFile = localVfs(".")
	actual fun userHomeVfs(): VfsFile = localVfs(".")
	actual fun tempVfs(): VfsFile = localVfs(tmpdir)
	actual fun localVfs(path: String): VfsFile = LocalVfsJvm()[path]

	actual val ResourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root }

	val tmpdir: String get() = System.getProperty("java.io.tmpdir")

	actual fun enterDebugger() = Unit
	actual fun printStackTrace(e: Throwable) = e.printStackTrace()
	actual fun log(msg: Any?): Unit = java.lang.System.out.println(msg)
	actual fun error(msg: Any?): Unit = java.lang.System.err.println(msg)

	actual suspend fun uncompressGzip(data: ByteArray): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		GZIPInputStream(ByteArrayInputStream(data)).copyTo(out)
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun uncompressZlib(data: ByteArray): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		InflaterInputStream(ByteArrayInputStream(data)).copyTo(out)
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun uncompressZlibRaw(data: ByteArray): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		InflaterInputStream(ByteArrayInputStream(data), java.util.zip.Inflater(true)).copyTo(out)
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun compressGzip(data: ByteArray, level: Int): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		val out2 = GZIPOutputStream(out)
		ByteArrayInputStream(data).copyTo(out2)
		out2.flush()
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun compressZlib(data: ByteArray, level: Int): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		val deflater = Deflater(level)
		val out2 = DeflaterOutputStream(out, deflater)
		ByteArrayInputStream(data).copyTo(out2)
		out2.flush()
		return@executeInWorker out.toByteArray()
	}

	actual suspend fun compressZlibRaw(data: ByteArray, level: Int): ByteArray = executeInWorker {
		val out = ByteArrayOutputStream()
		val deflater = Deflater(level, true)
		val out2 = DeflaterOutputStream(out, deflater)
		ByteArrayInputStream(data).copyTo(out2)
		out2.flush()
		return@executeInWorker out.toByteArray()
	}

	actual fun syncTest(block: suspend EventLoopTest.() -> Unit): Unit {
		sync(el = EventLoopTest(), step = 10, block = block)
	}
}
