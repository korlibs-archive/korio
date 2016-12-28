package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.copyTo
import com.soywiz.korio.stream.writeStream
import java.nio.charset.Charset

class VfsFile(
	val vfs: Vfs,
	path: String
) : VfsNamed(VfsFile.normalize(path)) {
	companion object {
		fun normalize(path: String): String = VfsUtil.normalize(path)
		fun combine(base: String, access: String): String = VfsUtil.combine(base, access)
	}

	operator fun get(path: String): VfsFile = VfsFile(vfs, combine(this.path, path))

	suspend operator fun set(path: String, content: String): Unit = this[path].writeString(content)
	suspend operator fun set(path: String, content: ByteArray): Unit = this[path].write(content)
	suspend operator fun set(path: String, content: AsyncStream): Unit = this[path].writeStream(content)
	suspend operator fun set(path: String, content: VfsFile): Unit = this[path].writeFile(content)

	suspend fun writeStream(src: AsyncStream): Unit = asyncFun {
		val dst = this.open(VfsOpenMode.CREATE)
		try {
			dst.writeStream(src)
		} finally {
			dst.close()
		}
	}

	suspend fun writeFile(file: VfsFile): Unit = file.copyTo(this)

	suspend fun copyTo(target: VfsFile): Unit = asyncFun {
		val src = this.open(VfsOpenMode.READ)
		try {
			target.writeStream(src)
		} finally {
			src.close()
		}
	}

	val parent: VfsFile by lazy { VfsFile(vfs, path.substringBeforeLast('/', "")) }
	val root: VfsFile get() = vfs.root

	suspend fun open(mode: VfsOpenMode = VfsOpenMode.READ): AsyncStream = vfs.open(path, mode)

	suspend inline fun <reified T : Any> readSpecial(noinline onProgress: (Long, Long) -> Unit): T = vfs.readSpecial(path, T::class.java, onProgress)
	suspend inline fun <reified T : Any> readSpecial(): T = vfs.readSpecial(path, T::class.java)

	suspend fun <T> readSpecial(clazz: Class<T>, onProgress: (Long, Long) -> Unit = { _, _ -> }): T = vfs.readSpecial(path, clazz, onProgress)

	suspend fun read(): ByteArray = vfs.readFully(path)
	suspend fun write(data: ByteArray): Unit = vfs.writeFully(path, data)

	suspend fun readString(charset: Charset = Charsets.UTF_8): String = asyncFun { vfs.readFully(path).toString(charset) }
	suspend fun writeString(data: String, charset: Charset = Charsets.UTF_8): Unit = asyncFun { vfs.writeFully(path, data.toByteArray(charset)) }

	suspend fun readChunk(offset: Long, size: Int): ByteArray = vfs.readChunk(path, offset, size)
	suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit = vfs.writeChunk(path, data, offset, resize)

	suspend fun stat(): VfsStat = vfs.stat(path)
	suspend fun size(): Long = asyncFun { vfs.stat(path).size }
	suspend fun exists(): Boolean = asyncFun {
		try {
			vfs.stat(path).exists
		} catch (e: Throwable) {
			false
		}
	}

	suspend fun isDirectory(): Boolean = asyncFun { stat().isDirectory }

	suspend fun setSize(size: Long): Unit = vfs.setSize(path, size)

	fun jail(): VfsFile = JailVfs(this)

	suspend fun delete() = vfs.delete(path)

	suspend fun mkdir() = vfs.mkdir(path)
	suspend fun mkdirs() = asyncFun {
		// @TODO: Create tree up to this
		mkdir()
	}

	suspend fun renameTo(dstPath: String) = vfs.rename(this.path, dstPath)

	suspend fun list(): AsyncSequence<VfsFile> = vfs.list(path)

	suspend fun listRecursive(): AsyncSequence<VfsFile> = asyncGenerate {
		for (file in list()) {
			yield(file)
			val stat = file.stat()
			if (stat.isDirectory) {
				for (file in file.listRecursive()) {
					yield(file)
				}
			}
		}
	}

	override fun toString(): String = "$vfs[$path]"
}