@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.spawn
import com.soywiz.korio.error.unsupported
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.copyTo
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.writeBytes
import com.soywiz.korio.util.use
import java.io.Closeable

abstract class Vfs {
	open protected val absolutePath: String = ""

	open fun getAbsolutePath(path: String) = VfsUtil.lightCombine(absolutePath, path)

	val root by lazy { VfsFile(this, "") }

	open val supportedAttributeTypes = listOf<Class<out Attribute>>()

	operator fun get(path: String) = root[path]

	fun file(path: String) = root[path]

	fun createExistsStat(
		path: String, isDirectory: Boolean, size: Long, device: Long = -1, inode: Long = -1, mode: Int = 511,
		owner: String = "nobody", group: String = "nobody", createTime: Long = 0L, modifiedTime: Long = createTime, lastAccessTime: Long = modifiedTime,
		extraInfo: Any? = null
	) = VfsStat(
		file = file(path), exists = true, isDirectory = isDirectory, size = size, device = device, inode = inode, mode = mode,
		owner = owner, group = group, createTime = createTime, modifiedTime = modifiedTime, lastAccessTime = lastAccessTime,
		extraInfo = extraInfo
	)

	fun createNonExistsStat(path: String, extraInfo: Any? = null) = VfsStat(file(path), exists = false, isDirectory = false, size = 0L, device = -1L, inode = -1L, mode = 511, owner = "nobody", group = "nobody", createTime = 0L, modifiedTime = 0L, lastAccessTime = 0L, extraInfo = extraInfo)

	suspend open fun exec(path: String, cmdAndArgs: List<String>, handler: VfsProcessHandler = VfsProcessHandler()): Int = throw UnsupportedOperationException()
	suspend open fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler = VfsProcessHandler()): Int = throw UnsupportedOperationException()

	suspend open fun open(path: String, mode: VfsOpenMode): AsyncStream = throw UnsupportedOperationException()

	suspend open fun readRange(path: String, range: LongRange): ByteArray {
		val s = open(path, VfsOpenMode.READ)
		try {
			s.position = range.start
			val readCount = Math.min(
				Int.MAX_VALUE.toLong() - 1,
				(range.endInclusive - range.start)
			).toInt() + 1

			return s.readBytes(readCount)
		} finally {
			s.close()
		}
	}

	interface Attribute

	inline fun <reified T> Iterable<Attribute>.get(): T? = this.firstOrNull { it is T } as T?

	suspend open fun put(path: String, content: AsyncStream, attributes: List<Attribute> = listOf()) {
		open(path, VfsOpenMode.CREATE_OR_TRUNCATE).use {
			content.copyTo(this)
		}
	}

	suspend fun readChunk(path: String, offset: Long, size: Int): ByteArray {
		val s = open(path, VfsOpenMode.READ)
		if (offset != 0L) s.setPosition(offset)
		return s.readBytes(size)
	}

	suspend fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean): Unit {
		val s = open(path, VfsOpenMode.CREATE)
		s.setPosition(offset)
		s.writeBytes(data)
		if (resize) s.setLength(s.getPosition())
	}

	suspend open fun setSize(path: String, size: Long): Unit {
		open(path, mode = VfsOpenMode.WRITE).use { this.setLength(size) }
	}

	suspend open fun setAttributes(path: String, attributes: List<Attribute>): Unit = Unit

	suspend open fun stat(path: String): VfsStat = createNonExistsStat(path)
	suspend open fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate { }
	suspend open fun mkdir(path: String, attributes: List<Attribute>): Boolean = unsupported()
	suspend open fun delete(path: String): Boolean = unsupported()
	suspend open fun rename(src: String, dst: String): Boolean = unsupported()
	suspend open fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = Closeable { }

	suspend open fun touch(path: String, time: Long, atime: Long) {
	}

	abstract class Proxy : Vfs() {
		abstract suspend protected fun access(path: String): VfsFile
		suspend open protected fun transform(out: VfsFile): VfsFile = file(out.path)
		suspend protected fun VfsFile.transform2(): VfsFile = transform(this)
		//suspend protected fun transform2_f(f: VfsFile): VfsFile = transform(f)

		suspend open protected fun init() {
		}

		var initialized = false
		suspend private fun initOnce(): Proxy {
			if (!initialized) {
				initialized = true
				init()
			}
			return this
		}

		suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int = initOnce().access(path).exec(cmdAndArgs, env, handler)
		suspend override fun open(path: String, mode: VfsOpenMode) = initOnce().access(path).open(mode)

		suspend override fun readRange(path: String, range: LongRange): ByteArray = initOnce().access(path).readRangeBytes(range)

		suspend override fun put(path: String, content: AsyncStream, attributes: List<Attribute>) = initOnce().access(path).put(content, attributes)
		suspend override fun setSize(path: String, size: Long): Unit = initOnce().access(path).setSize(size)
		suspend override fun stat(path: String): VfsStat = initOnce().access(path).stat().copy(file = file(path))
		suspend override fun list(path: String) = asyncGenerate { initOnce(); for (it in access(path).list()) yield(transform(it)) }
		suspend override fun delete(path: String): Boolean = initOnce().access(path).delete()
		suspend override fun setAttributes(path: String, attributes: List<Attribute>) = initOnce().access(path).setAttributes(*attributes.toTypedArray())
		suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean = initOnce().access(path).mkdir(*attributes.toTypedArray())
		suspend override fun touch(path: String, time: Long, atime: Long): Unit = initOnce().access(path).touch(time, atime)
		suspend override fun rename(src: String, dst: String): Boolean {
			initOnce()
			val srcFile = access(src)
			val dstFile = access(dst)
			if (srcFile.vfs != dstFile.vfs) throw IllegalArgumentException("Can't rename between filesystems. Use copyTo instead, and remove later.")
			return srcFile.renameTo(dstFile.path)
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
			initOnce()
			return access(path).watch { e ->
				spawn {
					val f1 = e.file.transform2()
					val f2 = e.other?.transform2()
					handler(e.copy(file = f1, other = f2))
				}
			}
		}
	}

	open class Decorator(val parent: VfsFile) : Vfs.Proxy() {
		val parentVfs = parent.vfs
		override suspend fun access(path: String): VfsFile = parentVfs[path]
	}
}
