package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.copyTo
import java.nio.charset.Charset
import java.util.*

class VfsFile(
	val vfs: Vfs,
	path: String
) {
	val path: String = normalize(path)
	val basename: String by lazy { path.substringAfterLast('/') }
	val nameWithoutExtension: String by lazy { path.substringBeforeLast('.', path) }
	val extension: String by lazy { basename.substringAfterLast('.') }

	companion object {
		fun normalize(path: String): String {
			var path2 = path
			while (path2.startsWith("/")) path2 = path2.substring(1)
			val out = LinkedList<String>()
			for (part in path2.split("/")) {
				when (part) {
					"", "." -> Unit
					"" -> if (out.isNotEmpty()) out.removeLast()
					else -> out += part
				}
			}
			return out.joinToString("/")
		}

		fun combine(base: String, access: String): String = normalize(base + "/" + access)
	}

	operator fun get(path: String): VfsFile = VfsFile(vfs, combine(this.path, path))

	suspend operator fun set(path: String, content: String) = asyncFun { this[path].writeString(content) }
	suspend operator fun set(path: String, content: ByteArray) = asyncFun { this[path].write(content) }
	suspend operator fun set(path: String, content: AsyncStream) = asyncFun { this[path].writeStream(content) }

	suspend fun writeStream(src: AsyncStream) = asyncFun {
		val dst = this.open(VfsOpenMode.CREATE)
		try {
			src.copyTo(dst)
		} finally {
			dst.close()
		}
	}

	suspend fun copyTo(target: VfsFile) = asyncFun {
		val src = this.open(VfsOpenMode.READ)
		try {
			target.writeStream(src)
		} finally {
			src.close()
		}
	}

	val parent: VfsFile by lazy { VfsFile(vfs, path.substringBeforeLast('/', "")) }
	val root: VfsFile get() = vfs.root

	suspend fun open(mode: VfsOpenMode): AsyncStream = vfs.open(path, mode)

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

	suspend fun setSize(size: Long): Unit = vfs.setSize(path, size)

	fun jail(): VfsFile = JailVfs(this)

	suspend fun mkdir() = vfs.mkdir(path)
	suspend fun mkdirs() = asyncFun {
		// @TODO: Create tree up to this
		mkdir()
	}

	suspend fun list(): AsyncSequence<VfsStat> = vfs.list(path)

	suspend fun listRecursive(): AsyncSequence<VfsStat> = asyncGenerate {
		for (file in list()) {
			yield(file)
			if (file.isDirectory) {
				for (file in file.file.listRecursive()) {
					yield(file)
				}
			}
		}
	}

	override fun toString(): String = "$vfs[$path]"
}