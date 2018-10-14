package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import java.io.*
import java.nio.*
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.nio.file.*
import java.nio.file.Path
import java.util.concurrent.*
import kotlin.coroutines.*

val ioContext by lazy { newFixedThreadPoolContext(2, "korioThreadPool") }
//suspend fun <T> executeIo(callback: suspend () -> T): T = withContext(ioContext) { callback() }
suspend fun <T> executeIo(callback: suspend () -> T): T = callback()

class LocalVfsJvm : LocalVfs() {
	val that = this
	override val absolutePath: String = ""

	fun resolve(path: String) = path
	fun resolvePath(path: String) = Paths.get(resolve(path))
	fun resolveFile(path: String) = File(resolve(path))

	override suspend fun exec(
		path: String,
		cmdAndArgs: List<String>,
		env: Map<String, String>,
		handler: VfsProcessHandler
	): Int = executeIo {
		val actualCmd = if (OS.isWindows) listOf("cmd", "/c") + cmdAndArgs else cmdAndArgs
		val pb = ProcessBuilder(actualCmd)
		pb.environment().putAll(lmapOf())
		pb.directory(resolveFile(path))

		val p = pb.start()
		var closing = false
		while (true) {
			val o = p.inputStream.readAvailableChunk(readRest = closing)
			val e = p.errorStream.readAvailableChunk(readRest = closing)
			if (o.isNotEmpty()) handler.onOut(o)
			if (e.isNotEmpty()) handler.onErr(e)
			if (closing) break
			if (o.isEmpty() && e.isEmpty() && !p.isAliveJre7) {
				closing = true
				continue
			}
			Thread.sleep(1L)
		}
		p.waitFor()
		//handler.onCompleted(p.exitValue())
		p.exitValue()
	}

	private fun InputStream.readAvailableChunk(readRest: Boolean): ByteArray {
		val out = ByteArrayOutputStream()
		while (if (readRest) true else available() > 0) {
			val c = this.read()
			if (c < 0) break
			out.write(c)
		}
		return out.toByteArray()
	}

	private fun InputStreamReader.readAvailableChunk(i: InputStream, readRest: Boolean): String {
		val out = java.lang.StringBuilder()
		while (if (readRest) true else i.available() > 0) {
			val c = this.read()
			if (c < 0) break
			out.append(c.toChar())
		}
		return out.toString()
	}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val channel = AsynchronousFileChannel.open(
			resolvePath(path), *when (mode) {
				VfsOpenMode.READ -> arrayOf(StandardOpenOption.READ)
				VfsOpenMode.WRITE -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE)
				VfsOpenMode.APPEND -> arrayOf(
					StandardOpenOption.READ,
					StandardOpenOption.WRITE,
					StandardOpenOption.APPEND
				)
				VfsOpenMode.CREATE -> arrayOf(
					StandardOpenOption.READ,
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE
				)
				VfsOpenMode.CREATE_NEW -> arrayOf(
					StandardOpenOption.READ,
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE_NEW
				)
				VfsOpenMode.CREATE_OR_TRUNCATE -> arrayOf(
					StandardOpenOption.READ,
					StandardOpenOption.WRITE,
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING
				)
			}
		)

		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				val bb = ByteBuffer.wrap(buffer, offset, len)
				return completionHandler<Int> { channel.read(bb, position, Unit, it) }
			}

			override suspend fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
				val bb = ByteBuffer.wrap(buffer, offset, len)
				completionHandler<Int> { channel.write(bb, position, Unit, it) }
			}

			override suspend fun setLength(value: Long): Unit {
				channel.truncate(value); Unit
			}

			override suspend fun getLength(): Long = channel.size()
			override suspend fun close() = channel.close()

			override fun toString(): String = "$that($path)"
		}.toAsyncStream()
	}

	override suspend fun setSize(path: String, size: Long): Unit = executeIo {
		val file = resolveFile(path)
		FileOutputStream(file, true).channel.use { outChan ->
			outChan.truncate(size)
		}
		Unit
	}

	override suspend fun stat(path: String): VfsStat = executeIo {
		val file = resolveFile(path)
		val fullpath = "$path/${file.name}"
		if (file.exists()) {
			val lastModified = file.lastModified()
			createExistsStat(
				fullpath,
				isDirectory = file.isDirectory,
				size = file.length(),
				createTime = lastModified,
				modifiedTime = lastModified,
				lastAccessTime = lastModified
			)
		} else {
			createNonExistsStat(fullpath)
		}
	}

	override suspend fun list(path: String): SuspendingSequence<VfsFile> =
		executeIo { (File(path).listFiles() ?: arrayOf()).map { that.file("$path/${it.name}") }.toAsync() }

	override suspend fun mkdir(path: String, attributes: List<Attribute>): Boolean =
		executeIo { resolveFile(path).mkdirs() }

	override suspend fun touch(path: String, time: Long, atime: Long): Unit =
		executeIo { resolveFile(path).setLastModified(time); Unit }

	override suspend fun delete(path: String): Boolean = executeIo { resolveFile(path).delete() }
	override suspend fun rmdir(path: String): Boolean = executeIo { resolveFile(path).delete() }
	override suspend fun rename(src: String, dst: String): Boolean =
		executeIo { resolveFile(src).renameTo(resolveFile(dst)) }

	suspend fun <T> completionHandler(callback: (CompletionHandler<T, Unit>) -> Unit): T {
		return suspendCancellableCoroutine<T> { c ->
			callback(object : CompletionHandler<T, Unit> {
				override fun completed(result: T, attachment: Unit?) = c.resume(result)
				override fun failed(exc: Throwable, attachment: Unit?) = c.resumeWithException(exc)
			})
		}
	}

	override suspend fun watch(path: String, handler: (FileEvent) -> Unit): Closeable {
		var running = true
		val fs = FileSystems.getDefault()
		val watcher = fs.newWatchService()

		fs.getPath(path).register(
			watcher,
			StandardWatchEventKinds.ENTRY_CREATE,
			StandardWatchEventKinds.ENTRY_DELETE,
			StandardWatchEventKinds.ENTRY_MODIFY
		)

		launchImmediately(coroutineContext) {
			while (running) {
				val key = executeIo {
					var r: WatchKey?
					do {
						r = watcher.poll(100L, TimeUnit.MILLISECONDS)
					} while (r == null && running)
					r
				} ?: continue

				for (e in key.pollEvents()) {
					val kind = e.kind()
					val filepath = e.context() as Path
					val rfilepath = fs.getPath(path, filepath.toString())
					val file = rfilepath.toFile()
					val absolutePath = file.absolutePath
					val vfsFile = file(absolutePath)
					when (kind) {
						StandardWatchEventKinds.OVERFLOW -> {
							println("Overflow WatchService")
						}
						StandardWatchEventKinds.ENTRY_CREATE -> {
							handler(
								FileEvent(
									FileEvent.Kind.CREATED,
									vfsFile
								)
							)
						}
						StandardWatchEventKinds.ENTRY_MODIFY -> {
							handler(
								FileEvent(
									FileEvent.Kind.MODIFIED,
									vfsFile
								)
							)
						}
						StandardWatchEventKinds.ENTRY_DELETE -> {
							handler(
								FileEvent(
									FileEvent.Kind.DELETED,
									vfsFile
								)
							)
						}
					}
				}
				key.reset()
			}
		}

		return Closeable {
			running = false
			watcher.close()
		}
	}

	override fun toString(): String = "LocalVfs"
}
