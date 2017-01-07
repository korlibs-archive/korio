package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readBytes
import com.soywiz.korio.stream.writeBytes

abstract class Vfs {
	open val absolutePath: String = ""
	val root by lazy { VfsFile(this, "") }

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

	suspend open fun open(path: String, mode: VfsOpenMode): AsyncStream = throw UnsupportedOperationException()

	//suspend open fun readFully(path: String) = asyncFun {
	//	val stat = stat(path)
	//	readChunk(path, 0L, stat.size.toInt())
	//}
	//
	//suspend open fun writeFully(path: String, data: ByteArray) = writeChunk(path, data, 0L, true)

	suspend fun readChunk(path: String, offset: Long, size: Int): ByteArray = asyncFun {
		val s = open(path, VfsOpenMode.READ)
		if (offset != 0L) s.setPosition(offset)
		s.readBytes(size)
	}

	suspend fun writeChunk(path: String, data: ByteArray, offset: Long, resize: Boolean): Unit = asyncFun {
		val s = open(path, VfsOpenMode.CREATE)
		s.setPosition(offset)
		s.writeBytes(data)
		if (resize) s.setLength(s.getPosition())
	}

	suspend open fun setSize(path: String, size: Long): Unit = asyncFun {
		open(path, mode = VfsOpenMode.WRITE).setLength(size)
	}

	suspend open fun stat(path: String): VfsStat = createNonExistsStat(path)

	/*
	suspend open fun stat(path: String): VfsStat = asyncFun {
		try {
			val len = open(path, VfsOpenMode.READ).use { getLength() }
			createExistsStat(path, isDirectory = false, len)
		} catch (e: Throwable) {
			createNonExistsStat(path)
		}
	}
	*/

	suspend open fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate { }
	suspend open fun mkdir(path: String): Boolean = throw UnsupportedOperationException()
	suspend open fun delete(path: String): Boolean = throw UnsupportedOperationException()
	suspend open fun rename(src: String, dst: String): Boolean = throw UnsupportedOperationException()

	abstract class Proxy : Vfs() {
		abstract suspend protected fun access(path: String): VfsFile
		suspend open protected fun transform(out: VfsFile): VfsFile {
			return file(out.path)
		}

		suspend open protected fun init() {
		}

		private var initialized = false
		suspend private fun initOnce() = asyncFun {
			if (!initialized) {
				initialized = true
				init()
			}
		}

		suspend override fun exec(path: String, cmdAndArgs: List<String>, handler: VfsProcessHandler): Int = asyncFun { initOnce(); access(path).exec(cmdAndArgs, handler) }

		suspend override fun open(path: String, mode: VfsOpenMode) = asyncFun { initOnce(); access(path).open(mode) }

		//suspend override fun readFully(path: String): ByteArray = asyncFun { initOnce(); access(path).read() }
		//suspend override fun writeFully(path: String, data: ByteArray): Unit = asyncFun { initOnce(); access(path).write(data) }
		suspend override fun setSize(path: String, size: Long): Unit = asyncFun { initOnce(); access(path).setSize(size) }

		suspend override fun stat(path: String): VfsStat = asyncFun { initOnce(); access(path).stat().copy(file = file(path)) }
		suspend override fun list(path: String) = asyncGenerate {
			initOnce()
			for (it in access(path).list()) yield(transform(it))
		}

		suspend override fun delete(path: String): Boolean = asyncFun { initOnce(); access(path).delete() }
		suspend override fun mkdir(path: String): Boolean = asyncFun { initOnce(); access(path).mkdir() }
		suspend override fun rename(src: String, dst: String): Boolean = asyncFun {
			initOnce()
			val srcFile = access(src)
			val dstFile = access(dst)
			if (srcFile.vfs != dstFile.vfs) throw IllegalArgumentException("Can't rename between filesystems. Use copyTo instead, and remove later.")
			srcFile.renameTo(dstFile.path)
		}
	}

	open class Decorator(val parent: VfsFile) : Vfs.Proxy() {
		val parentVfs = parent.vfs
		override suspend fun access(path: String): VfsFile = parentVfs[path]
	}
}