package com.soywiz.korio

import com.soywiz.korio.async.EventLoopFactory
import com.soywiz.korio.async.EventLoopFactoryJvmAndCSharp
import com.soywiz.korio.async.executeInWorkerSafer
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.JvmAsyncSocketFactory
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpClientJvm
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.ws.WebSocketClientFactory
import com.soywiz.korio.vfs.LocalVfsProvider
import com.soywiz.korio.vfs.LocalVfsProviderJvm
import com.soywiz.korio.vfs.ResourcesVfsProviderJvm
import com.soywiz.korio.vfs.VfsFile
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.*
import java.util.zip.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.reflect.KClass

actual typealias Synchronized = kotlin.jvm.Synchronized
actual typealias JvmField = kotlin.jvm.JvmField
actual typealias JvmStatic = kotlin.jvm.JvmStatic
actual typealias JvmOverloads = kotlin.jvm.JvmOverloads
actual typealias Transient = kotlin.jvm.Transient

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

actual typealias RuntimeException = java.lang.RuntimeException
actual typealias IllegalStateException = java.lang.IllegalStateException
actual typealias CancellationException = java.util.concurrent.CancellationException

actual object KorioNative {
	actual fun currentTimeMillis() = System.currentTimeMillis()
	actual fun getLocalTimezoneOffset(time: Long): Int {
		return TimeZone.getDefault().getOffset(time)
	}

	actual suspend fun <T> executeInWorker(callback: suspend () -> T): T {
		return executeInWorkerSafer(callback)
	}


	actual val platformName: String by lazy {
		"jvm"
	}

	actual val osName: String by lazy {
		System.getProperty("os.name")
	}

	actual object DefaultHttpFactoryFactory {
		actual fun createFactory(): HttpFactory = object : HttpFactory {
			init {
				System.setProperty("http.keepAlive", "false")
			}

			override fun createClient(): HttpClient = HttpClientJvm()

			override fun createServer(): HttpServer = TODO()
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

	actual object CreateAnnotation {
		actual fun <T : Any> createAnnotation(clazz: KClass<T>, map: Map<String, Any?>): T {
			val kclass = (clazz as kotlin.reflect.KClass<Any>)
			return Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(kclass.java)) { proxy, method, args ->
				map[method.name]
			} as T
		}
	}

	actual class SimplerMessageDigest actual constructor(name: String) {
		val md = MessageDigest.getInstance(name)

		actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
			md.update(data, offset, size)
		}

		actual suspend fun digest(): ByteArray = executeInWorkerSafer {
			md.digest()
		}
	}

	actual class SimplerMac actual constructor(name: String, key: ByteArray) {
		val mac = Mac.getInstance(name).apply {
			init(SecretKeySpec(key, name))
		}

		actual suspend fun update(data: ByteArray, offset: Int, size: Int) = executeInWorkerSafer {
			mac.update(data, offset, size)
		}

		actual suspend fun finalize(): ByteArray = executeInWorkerSafer {
			mac.doFinal()
		}
	}

	actual object SyncCompression {
		actual fun inflate(data: ByteArray): ByteArray {
			val out = ByteOutputStream()
			val s = InflaterInputStream(ByteArrayInputStream(data))
			val temp = ByteArray(0x1000)
			while (true) {
				val read = s.read(temp, 0, temp.size)
				if (read <= 0) break
				out.write(temp, 0, read)
			}
			return out.bytes.copyOf(out.count)
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
	actual val localVfsProvider: LocalVfsProvider by lazy { LocalVfsProviderJvm() }
	actual val ResourcesVfs: VfsFile by lazy { ResourcesVfsProviderJvm()().root }

	actual val tmpdir: String get() = System.getProperty("java.io.tmpdir")

	actual fun <T> copyRangeTo(src: Array<T>, srcPos: Int, dst: Array<T>, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: BooleanArray, srcPos: Int, dst: BooleanArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: ByteArray, srcPos: Int, dst: ByteArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: ShortArray, srcPos: Int, dst: ShortArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: IntArray, srcPos: Int, dst: IntArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: LongArray, srcPos: Int, dst: LongArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: FloatArray, srcPos: Int, dst: FloatArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun copyRangeTo(src: DoubleArray, srcPos: Int, dst: DoubleArray, dstPos: Int, count: Int) {
		System.arraycopy(src, srcPos, dst, dstPos, count)
	}

	actual fun <T> fill(src: Array<T>, value: T, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: BooleanArray, value: Boolean, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: ByteArray, value: Byte, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: ShortArray, value: Short, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: IntArray, value: Int, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: FloatArray, value: Float, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun fill(src: DoubleArray, value: Double, from: Int, to: Int) {
		java.util.Arrays.fill(src, from, to, value)
	}

	actual fun enterDebugger() {}

	actual fun log(msg: Any?): Unit {
		java.lang.System.out.println(msg)
	}

	actual fun error(msg: Any?): Unit {
		java.lang.System.err.println(msg)
	}

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

	actual class FastMemory(val buffer: ByteBuffer, actual val size: Int) {
		val i16 = buffer.asShortBuffer()
		val i32 = buffer.asIntBuffer()
		val f32 = buffer.asFloatBuffer()

		companion actual object {
			actual fun alloc(size: Int): FastMemory {
				return FastMemory(ByteBuffer.allocate((size + 0xF) and 0xF.inv()).order(ByteOrder.nativeOrder()), size)
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
			return buffer.get(index).toInt() and 0xFF
		}

		actual operator fun set(index: Int, value: Int): Unit {
			buffer.put(index, value.toByte())
		}

		actual fun setAlignedInt16(index: Int, value: Short): Unit = run { i16.put(index, value) }
		actual fun getAlignedInt16(index: Int): Short = i16.get(index)
		actual fun setAlignedInt32(index: Int, value: Int): Unit = run { i32.put(index, value) }
		actual fun getAlignedInt32(index: Int): Int = i32.get(index)
		actual fun setAlignedFloat32(index: Int, value: Float): Unit = run { f32.put(index, value) }
		actual fun getAlignedFloat32(index: Int): Float = f32.get(index)

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

		actual fun getInt16(index: Int): Short = buffer.getShort(index)
		actual fun getInt32(index: Int): Int = buffer.getInt(index)
		actual fun getFloat32(index: Int): Float = buffer.getFloat(index)
	}
}


/*
class LocalVfsProviderCSharp : LocalVfsProvider() {
	override fun invoke(): LocalVfs = CSharpVisualVfs()
}

@JTranscAddMembers(target = "cs", value = """
	System.IO.FileStream fs;
""")
class CSharpFileAsyncStream(val path: String, val mode: VfsOpenMode) : AsyncStreamBase() {
	init {
		_init(path)
	}

	private fun _init(path: String) = CSharp.v_raw("fs = System.IO.File.Open(N.istr(p0), System.IO.FileMode.Open, System.IO.FileAccess.Read, System.IO.FileShare.None);")

	private fun _setLength(p0: Long) = CSharp.v_raw("fs.Length = p0")
	private fun _getLength(): Long = CSharp.l_raw("fs.Length")
	private fun _close() = CSharp.v_raw("fs.Close();")

	private fun _read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
		CSharp.v_raw("fs.Position = p0;")
		return CSharp.i_raw("fs.Read(p1.u(), p2, p3);")
	}

	private fun _write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
		CSharp.v_raw("fs.Position = p0;")
		CSharp.v_raw("fs.Write(p1.u(), p2, p3);")
	}

	suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = CSharp.runTaskAsync { _read(position, buffer, offset, len) }
	suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = CSharp.runTaskAsync { _write(position, buffer, offset, len) }
	suspend override fun setLength(value: Long) = CSharp.runTaskAsync { _setLength(value) }
	suspend override fun getLength(): Long = CSharp.runTaskAsync { _getLength() }
	suspend override fun close() = CSharp.runTaskAsync { _close() }
}

class CSharpVisualVfs : LocalVfs() {
	override fun getAbsolutePath(path: String): String = path

	override val supportedAttributeTypes = listOf<Class<out Attribute>>()

	suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
		TODO()
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = CSharpFileAsyncStream(path, mode).toAsyncStream()

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		TODO()
	}

	//private fun getFileInfo(path: String) = CSharp.raw<Any>("N.wrap(new System.IO.FileInfo(N.istr(p0)))")
	//private fun fileInfoExists(data: Any) = CSharp.b_raw("((System.IO.FileInfo)N.unwrap(p0)).Exists")
	//private fun fileInfoSize(data: Any) = CSharp.l_raw("((System.IO.FileInfo)N.unwrap(p0)).Length")
	//private fun fileInfoIsDirectory(data: Any) = CSharp.b_raw("((System.IO.FileInfo)N.unwrap(p0)).Length")

	private fun fileSize(path: String) = CSharp.l_raw("(new System.IO.FileInfo(N.istr(p0))).Length")
	private fun fileExists(path: String) = CSharp.b_raw("System.IO.File.Exists(N.istr(p0))")
	private fun directoryExists(path: String) = CSharp.b_raw("System.IO.Directory.Exists(N.istr(p0))")

	suspend override fun stat(path: String): VfsStat {
		return CSharp.runTaskAsync {
			val fileExists = fileExists(path)
			val directoryExists = directoryExists(path)
			if (fileExists) {
				createExistsStat(path, isDirectory = false, size = fileSize(path))
			} else if (directoryExists) {
				createExistsStat(path, isDirectory = true, size = 0L)
			} else {
				createNonExistsStat(path)
			}
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> {
		TODO()
	}

	@JTranscMethodBody(target = "cs", value = """
		var path = N.istr(p0);
		try {
			var res = !System.IO.Directory.Exists(path);
			System.IO.Directory.CreateDirectory(path);
			return res;
		} catch (Exception) {
			return false;
		}
	""")
	external private fun createDirectory(p0: String): Boolean

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		return CSharp.runTaskAsync { createDirectory(path) }
	}

	suspend override fun delete(path: String): Boolean {
		TODO()
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		TODO()
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		TODO()
	}
}

suspend fun <T> CSharp.runTaskAsync(task: () -> T): T {
	val deferred = Promise.Deferred<T>()

	CSharp.runTask {
		deferred.resolve(task())
	}

	return deferred.promise.await()
}

class LocalVfsProviderHaxe : LocalVfsProvider() {
	override val available: Boolean = JTranscSystem.isHaxe()

	override fun invoke(): LocalVfs {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}

class HttpFactoryCSharp : HttpFactory {
	override fun createClient(): HttpClient = CSharpHttpClient()
}

class CSharpHttpClient : HttpClient() {
	private val csClient = CSharp.raw<Any>("N.wrap(new System.Net.Http.HttpClient())")

	private fun createCSharpHttpMethod(kind: String) = CSharp.raw<String>("N.str(new System.Net.Http.HttpMethod(N.istr(p0)))")

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		// System.Net.Http.HttpClient
		// OpenReadAsync
		// OpenWriteAsync
		TODO()
	}
}
*/