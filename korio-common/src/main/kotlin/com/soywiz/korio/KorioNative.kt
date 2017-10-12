package com.soywiz.korio

import com.soywiz.korio.async.EventLoopFactory
import com.soywiz.korio.net.AsyncSocketFactory
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.net.ws.WebSocketClientFactory
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
	val platformName: String
	val osName: String
	val ResourcesVfs: VfsFile

	val websockets: WebSocketClientFactory

	fun currentTimeMillis(): Long

	val eventLoopFactoryDefaultImpl: EventLoopFactory

	suspend fun <T> executeInWorker(callback: suspend () -> T): T

	val File_separatorChar: Char

	val localVfsProvider: LocalVfsProvider
	val tmpdir: String

	fun Thread_sleep(time: Long): Unit

	val asyncSocketFactory: AsyncSocketFactory

	object DefaultHttpFactoryFactory {
		fun createFactory(): HttpFactory
	}

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

	class UTCDate {
		companion object {
			operator fun invoke(fullYear: Int, month0: Int, day: Int, hours: Int, minutes: Int, seconds: Int): UTCDate
			operator fun invoke(time: Long): UTCDate
		}

		val time: Long
		val fullYear: Int
		val dayOfMonth: Int
		val dayOfWeek: Int
		val month0: Int
		val hours: Int
		val minutes: Int
		val seconds: Int
	}
}
