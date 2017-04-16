@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.jvm

import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.korioSuspendCoroutine
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.isAliveJre7
import com.soywiz.korio.vfs.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit


class LocalVfsProviderJvm : LocalVfsProvider() {
	override fun invoke(): Vfs = object : Vfs() {
		val that = this
		override val absolutePath: String = ""

		fun resolve(path: String) = path
		fun resolvePath(path: String) = Paths.get(resolve(path))
		fun resolveFile(path: String) = File(resolve(path))

		suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int = executeInWorker {
			val actualCmd = if (OS.isWindows) listOf("cmd", "/c") + cmdAndArgs else cmdAndArgs
			val pb = ProcessBuilder(actualCmd)
			pb.environment().putAll(mapOf())
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

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val channel = AsynchronousFileChannel.open(resolvePath(path), *when (mode) {
				VfsOpenMode.READ -> arrayOf(StandardOpenOption.READ)
				VfsOpenMode.WRITE -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE)
				VfsOpenMode.APPEND -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
				VfsOpenMode.CREATE -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
				VfsOpenMode.CREATE_NEW -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)
				VfsOpenMode.CREATE_OR_TRUNCATE -> arrayOf(StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
			})

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val bb = ByteBuffer.wrap(buffer, offset, len)
					return completionHandler<Int> { channel.read(bb, position, Unit, it) }
				}

				suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) {
					val bb = ByteBuffer.wrap(buffer, offset, len)
					completionHandler<Int> { channel.write(bb, position, Unit, it) }
				}

				suspend override fun setLength(value: Long): Unit {
					channel.truncate(value); Unit
				}

				suspend override fun getLength(): Long = channel.size()
				suspend override fun close() = channel.close()

				override fun toString(): String = "$that($path)"
			}.toAsyncStream()
		}

		suspend override fun setSize(path: String, size: Long): Unit = executeInWorker {
			val file = resolveFile(path)
			FileOutputStream(file, true).channel.use { outChan ->
				outChan.truncate(size)
			}
			Unit
		}

		suspend override fun stat(path: String): VfsStat = executeInWorker {
			val file = resolveFile(path)
			val fullpath = "$path/${file.name}"
			if (file.exists()) {
				createExistsStat(
					fullpath,
					isDirectory = file.isDirectory,
					size = file.length()
				)
			} else {
				createNonExistsStat(fullpath)
			}
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> {
			/*
			val emitter = AsyncSequenceEmitter<VfsFile>()
			val files = executeInWorker { Files.newDirectoryStream(resolvePath(path)) }
			spawnAndForget {
				executeInWorker {
					try {
						for (p in files.toList().sortedBy { it.toFile().name }) {
							val file = p.toFile()
							emitter.emit(VfsFile(that, file.absolutePath))
						}
					} finally {
						emitter.close()
					}
				}
			}
			return emitter.toSequence()
			*/
			return executeInWorker {
				asyncGenerate {
					for (file in File(path).listFiles() ?: arrayOf()) {
						yield(that.file("$path/${file.name}"))
					}
				}
			}
		}

		suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean = executeInWorker {
			resolveFile(path).mkdir()
		}

		suspend override fun touch(path: String, time: Long, atime: Long) {
			resolveFile(path).setLastModified(time)
		}

		suspend override fun delete(path: String): Boolean = executeInWorker {
			resolveFile(path).delete()
		}

		suspend override fun rename(src: String, dst: String): Boolean = executeInWorker {
			resolveFile(src).renameTo(resolveFile(dst))
		}

		inline suspend fun <T> completionHandler(crossinline callback: (CompletionHandler<T, Unit>) -> Unit) = korioSuspendCoroutine<T> { c ->
			val cevent = c.toEventLoop()
			callback(object : CompletionHandler<T, Unit> {
				override fun completed(result: T, attachment: Unit?) = cevent.resume(result)
				override fun failed(exc: Throwable, attachment: Unit?) = cevent.resumeWithException(exc)
			})
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
			var running = true
			val fs = FileSystems.getDefault()
			val watcher = fs.newWatchService()

			fs.getPath(path).register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

			spawnAndForget {
				while (running) {
					val key = executeInWorker {
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
								handler(VfsFileEvent(VfsFileEvent.Kind.CREATED, vfsFile))
							}
							StandardWatchEventKinds.ENTRY_MODIFY -> {
								handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, vfsFile))
							}
							StandardWatchEventKinds.ENTRY_DELETE -> {
								handler(VfsFileEvent(VfsFileEvent.Kind.DELETED, vfsFile))
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
}

