@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs

import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.async
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.await
import com.soywiz.korio.coroutine.withCoroutineContext
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.LONG_ZERO_TO_MAX_RANGE
import com.soywiz.korio.util.toLongRange
import com.soywiz.korio.util.use
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.nio.charset.Charset

class VfsFile(
	val vfs: Vfs,
	path: String
//) : VfsNamed(VfsFile.normalize(path)) {
) : VfsNamed(path), AsyncInputOpenable {
	operator fun get(path: String): VfsFile = VfsFile(vfs, VfsUtil.combine(this.path, path))

	// @TODO: Kotlin suspend operator not supported yet!
	suspend fun set(path: String, content: String): Unit = this[path].put(content.toByteArray(Charsets.UTF_8).openAsync())

	suspend fun set(path: String, content: ByteArray): Unit = this[path].put(content.openAsync())
	suspend fun set(path: String, content: AsyncStream): Unit = run { this[path].writeStream(content) }
	suspend fun set(path: String, content: VfsFile): Unit = run { this[path].writeFile(content) }

	suspend fun put(content: AsyncStream, attributes: List<Vfs.Attribute> = listOf()): Unit = vfs.put(path, content, attributes)
	suspend fun put(content: AsyncStream, vararg attributes: Vfs.Attribute): Unit = vfs.put(path, content, attributes.toList())
	suspend fun write(data: ByteArray, vararg attributes: Vfs.Attribute): Unit = put(data.openAsync(), *attributes)

	suspend fun writeStream(src: AsyncStream, vararg attributes: Vfs.Attribute): Long = run { put(src, *attributes); src.getLength() }
	suspend fun writeFile(file: VfsFile, vararg attributes: Vfs.Attribute): Long = file.copyTo(this, *attributes)

	suspend fun copyTo(target: VfsFile, vararg attributes: Vfs.Attribute): Long = this.openUse(VfsOpenMode.READ) { target.writeStream(this, *attributes) }

	val parent: VfsFile by lazy { VfsFile(vfs, pathInfo.folder) }
	val root: VfsFile get() = vfs.root

	fun withExtension(ext: String): VfsFile = VfsFile(vfs, fullnameWithoutExtension + if (ext.isNotEmpty()) ".$ext" else "")
	fun appendExtension(ext: String): VfsFile = VfsFile(vfs, fullname + ".$ext")

	suspend fun open(mode: VfsOpenMode = VfsOpenMode.READ): AsyncStream = vfs.open(path, mode)

	suspend override fun openRead(): AsyncStream = open(VfsOpenMode.READ)

	suspend inline fun <T> openUse(mode: VfsOpenMode = VfsOpenMode.READ, noinline callback: suspend AsyncStream.() -> T): T {
		return open(mode).use { callback.await(this) }
	}

	inline suspend fun <reified T> readSpecial(): T = vfs.readSpecial(path, T::class.java)
	suspend fun <T> readSpecial(clazz: Class<T>): T = vfs.readSpecial(path, clazz)
	suspend fun readRangeBytes(range: LongRange): ByteArray = vfs.readRange(path, range)
	suspend fun readRangeBytes(range: IntRange): ByteArray = vfs.readRange(path, range.toLongRange())

	// Aliases
	suspend fun read(): ByteArray = vfs.readRange(path, LONG_ZERO_TO_MAX_RANGE)

	suspend fun readAll(): ByteArray = vfs.readRange(path, LONG_ZERO_TO_MAX_RANGE)
	suspend fun readBytes(): ByteArray = vfs.readRange(path, LONG_ZERO_TO_MAX_RANGE)

	suspend fun readAsSyncStream(): SyncStream = read().openSync()

	suspend fun readString(charset: Charset = Charsets.UTF_8): String = read().toString(charset)
	suspend fun writeString(data: String, vararg attributes: Vfs.Attribute): Unit = write(data.toByteArray(Charsets.UTF_8), *attributes)
	suspend fun writeString(data: String, charset: Charset, vararg attributes: Vfs.Attribute): Unit = write(data.toByteArray(charset), *attributes)

	suspend fun readChunk(offset: Long, size: Int): ByteArray = vfs.readChunk(path, offset, size)
	suspend fun writeChunk(data: ByteArray, offset: Long, resize: Boolean = false): Unit = vfs.writeChunk(path, data, offset, resize)

	suspend fun stat(): VfsStat = vfs.stat(path)
	suspend fun touch(time: Long, atime: Long = time): Unit = vfs.touch(path, time, atime)
	suspend fun size(): Long = vfs.stat(path).size
	suspend fun exists(): Boolean = try {
		vfs.stat(path).exists
	} catch (e: Throwable) {
		false
	}

	suspend fun isDirectory(): Boolean = stat().isDirectory

	suspend fun setSize(size: Long): Unit = vfs.setSize(path, size)

	fun jail(): VfsFile = JailVfs(this)

	suspend fun delete() = vfs.delete(path)

	suspend fun setAttributes(attributes: List<Vfs.Attribute>) = vfs.setAttributes(path, attributes)
	suspend fun setAttributes(vararg attributes: Vfs.Attribute) = vfs.setAttributes(path, attributes.toList())

	suspend fun mkdir(attributes: List<Vfs.Attribute>) = vfs.mkdir(path, attributes)
	suspend fun mkdirs(attributes: List<Vfs.Attribute>) {
		// @TODO: Create tree up to this
		mkdir(attributes)
	}

	suspend fun mkdir(vararg attributes: Vfs.Attribute) = mkdir(attributes.toList())
	suspend fun mkdirs(vararg attributes: Vfs.Attribute) = mkdirs(attributes.toList())

	suspend fun copyToTree(target: VfsFile, vararg attributes: Vfs.Attribute, notify: suspend (Pair<VfsFile, VfsFile>) -> Unit = {}): Unit {
		notify(this to target)
		if (this.isDirectory()) {
			target.mkdir()
			for (file in list()) {
				file.copyToTree(target[file.basename], *attributes, notify = notify)
			}
		} else {
			//println("copyToTree: $this -> $target")
			this.copyTo(target, *attributes)
		}
	}

	suspend fun ensureParents() = this.apply { parent.mkdirs() }

	suspend fun renameTo(dstPath: String) = vfs.rename(this.path, dstPath)

	suspend fun list(): AsyncSequence<VfsFile> = vfs.list(path)

	suspend fun listRecursive(filter: (VfsFile) -> Boolean = { true }): AsyncSequence<VfsFile> = withCoroutineContext {
		asyncGenerate(this@withCoroutineContext) {
			for (file in list()) {
				if (!filter(file)) continue
				yield(file)
				val stat = file.stat()
				if (stat.isDirectory) {
					for (f in file.listRecursive()) yield(f)
				}
			}
		}
	}

	suspend fun exec(cmdAndArgs: List<String>, env: Map<String, String> = mapOf(), handler: VfsProcessHandler = VfsProcessHandler()): Int = vfs.exec(path, cmdAndArgs, env, handler)
	suspend fun execToString(cmdAndArgs: List<String>, env: Map<String, String> = mapOf(), charset: Charset = Charsets.UTF_8, captureError: Boolean = false): String {
		val out = ByteArrayOutputStream()

		val result = exec(cmdAndArgs, env, object : VfsProcessHandler() {
			suspend override fun onOut(data: ByteArray) {
				out.write(data)
			}

			suspend override fun onErr(data: ByteArray) {
				if (captureError) out.write(data)
			}
		})

		if (result != 0) throw VfsProcessException("Process not returned 0, but $result")

		return out.toByteArray().toString(charset)
	}

	suspend fun execToString(vararg cmdAndArgs: String, charset: Charset = Charsets.UTF_8): String = execToString(cmdAndArgs.toList(), charset = charset)

	suspend fun passthru(cmdAndArgs: List<String>, env: Map<String, String> = mapOf(), charset: Charset = Charsets.UTF_8): Int {
		return exec(cmdAndArgs.toList(), env, object : VfsProcessHandler() {
			suspend override fun onOut(data: ByteArray) {
				System.out.print(data.toString(charset))
			}

			suspend override fun onErr(data: ByteArray) {
				System.err.print(data.toString(charset))
			}
		})
	}

	suspend fun passthru(vararg cmdAndArgs: String, env: Map<String, String> = mapOf(), charset: Charset = Charsets.UTF_8): Int = passthru(cmdAndArgs.toList(), env, charset)

	suspend fun watch(handler: suspend (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
		vfs.watch(path) { event -> async { handler(event) } }
	}

	suspend fun redirected(pathRedirector: suspend VfsFile.(String) -> String): VfsFile {
		val actualFile = this
		return VfsFile(object : Vfs.Proxy() {
			suspend override fun access(path: String): VfsFile = actualFile[actualFile.pathRedirector(path)]
		}, path)
	}

	override fun toString(): String = "$vfs[$path]"

	val absolutePath: String by lazy { vfs.getAbsolutePath(path) }
}