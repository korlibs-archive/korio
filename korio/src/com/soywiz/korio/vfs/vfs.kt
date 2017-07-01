package com.soywiz.korio.vfs

import com.jtransc.io.JTranscFileMode
import com.jtransc.io.JTranscFileStat
import com.jtransc.io.async.*
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.stream.*
import java.io.Closeable
import java.nio.file.FileAlreadyExistsException

class LocalVfsProviderJTransc : LocalVfsProvider() {
	override fun invoke(): LocalVfs = LocalVfsJTransc()
}

private fun JTranscFileStat.toVfsStat(vfs: Vfs): VfsStat {
	return VfsStat(VfsFile(vfs, this.path), this.exists, this.isDirectory, this.length)
}

private fun VfsOpenMode.toJTranscOpenMode() = when (this) {
	VfsOpenMode.READ -> JTranscFileMode.READ
	VfsOpenMode.WRITE -> JTranscFileMode.WRITE
	VfsOpenMode.CREATE -> JTranscFileMode.WRITE
	VfsOpenMode.CREATE_NEW -> JTranscFileMode.WRITE
	VfsOpenMode.CREATE_OR_TRUNCATE -> JTranscFileMode.WRITE
	VfsOpenMode.APPEND -> JTranscFileMode.APPEND
}

private fun JTranscAsyncStream.toAsyncStream(): AsyncStream {
	val fs = this
	return object : AsyncStreamBase() {
		suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = fs.read(position, buffer, offset, len)
		suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int): Unit = run { fs.write(position, buffer, offset, len) }
		suspend override fun setLength(value: Long): Unit = run { fs.setLength(value) }
		suspend override fun getLength(): Long = fs.getLength()
		suspend override fun close(): Unit = run { fs.close() }
	}.toAsyncStream()
}

class LocalVfsJTransc : LocalVfs() {
	override fun getAbsolutePath(path: String): String = path

	override val supportedAttributeTypes = listOf<Class<out Attribute>>()

	suspend override fun exec(path: String, cmdAndArgs: List<String>, env: Map<String, String>, handler: VfsProcessHandler): Int {
		TODO()
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		if (mode.createIfNotExists && this[path].exists()) throw FileAlreadyExistsException(path)
		val fs = JTranscAsyncFile(path).open(mode.toJTranscOpenMode())
		if (mode.truncate) fs.setLength(0L)
		return fs.toAsyncStream()
	}

	suspend override fun setAttributes(path: String, attributes: List<Attribute>) {
		TODO()
	}

	suspend override fun stat(path: String): VfsStat = JTranscAsyncFile(path).stat().toVfsStat(this)

	suspend override fun list(path: String): AsyncSequence<VfsFile> = asyncGenerate {
		for (it in jtranscAsyncFileSystem.list(path)) {
			yield(VfsFile(this@LocalVfsJTransc, "$path/$it"))
		}
	}

	suspend override fun mkdir(path: String, attributes: List<Attribute>): Boolean {
		return jtranscAsyncFileSystem.mkdir(path)
	}

	suspend override fun delete(path: String): Boolean {
		return jtranscAsyncFileSystem.delete(path)
	}

	suspend override fun rename(src: String, dst: String): Boolean {
		return jtranscAsyncFileSystem.rename(src, dst)
	}

	suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
		TODO()
	}
}

class ResourcesVfsProviderJTransc : ResourcesVfsProvider() {
	override operator fun invoke(classLoader: ClassLoader): Vfs {
		return object : Vfs() {
			suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
				return jtranscAsyncResources.open(path.trim('/'), classLoader).toAsyncStream().readAll().openAsync()
			}

			suspend override fun stat(path: String): VfsStat {
				return jtranscAsyncResources.stat(path.trim('/'), classLoader).toVfsStat(this)
			}
		}
	}
}


/*

class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val stat = fstat(path)
			val handle = open(path, "r")

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val data = read(handle, position.toDouble(), len.toDouble())
					System.arraycopy(data, 0, buffer, offset, data.size)
					return data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = try {
			val stat = fstat(path)
			createExistsStat(path, isDirectory = stat.isDirectory, size = stat.size.toLong())
		} catch (t: Throwable) {
			createNonExistsStat(path)
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> {
			val emitter = AsyncSequenceEmitter<VfsFile>()
			val fs = jsRequire("fs")
			//console.methods["log"](path)
			fs.call("readdir", path, jsFunctionRaw2 { err, files ->
				//console.methods["log"](err)
				//console.methods["log"](files)
				for (n in 0 until files["length"].toInt()) {
					val file = files[n].toJavaString()
					//println("::$file")
					emitter(file("$path/$file"))
				}
				emitter.close()
			})
			return emitter.toSequence()
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
			val fs = jsRequire("fs")
			val watcher = fs.call("watch", path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
				spawnAndForget(this@withCoroutineContext) {
					val et = eventType.toJavaString()
					val fn = filename.toJavaString()
					val f = file("$path/$fn")
					//println("$et, $fn")
					when (et) {
						"rename" -> {
							val kind = if (f.exists()) VfsFileEvent.Kind.CREATED else VfsFileEvent.Kind.DELETED
							handler(VfsFileEvent(kind, f))
						}
						"change" -> {
							handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, f))
						}
						else -> {
							println("Unhandled event: $et")
						}
					}
				}
			})

			return@withCoroutineContext Closeable { watcher.call("close") }
		}

		override fun toString(): String = "LocalVfs"
	}


	suspend fun open(path: String, mode: String): JsDynamic = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.call("open", path, mode, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				c.resume(fd!!)
			}
		})
	}

	suspend fun read(fd: JsDynamic?, position: Double, len: Double): ByteArray = Promise.create { c ->
		val fs = jsRequire("fs")
		val buffer = jsNew("Buffer", len)
		fs.call("read", fd, buffer, 0, len, position, jsFunctionRaw3 { err, bytesRead, buffer ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening ${fd.toJavaString()}"))
			} else {
				val u8array = jsNew("Int8Array", buffer, 0, bytesRead)
				val out = ByteArray(bytesRead.toInt())
				out.asJsDynamic().call("setArraySlice", 0, u8array)
				c.resolve(out)
			}
		})
	}

	suspend fun close(fd: Any): Unit = Promise.create { c ->
		val fs = jsRequire("fs")
		fs.call("close", fd, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} closing file"))
			} else {
				c.resolve(Unit)
			}
		})
	}



	suspend fun fstat(path: String): JsStat = Promise.create { c ->
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
		val fs = jsRequire("fs")
		//fs.methods["exists"](path, jsFunctionRaw1 { jsexists ->
		//	val exists = jsexists.toBool()
		//	if (exists) {
		fs.call("stat", path, jsFunctionRaw2 { err, stat ->
			//console.methods["log"](stat)
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				val out = JsStat(stat["size"].toDouble())
				out.isDirectory = stat.call("isDirectory").toBool()
				c.resolve(out)
			}
		})
		//	} else {
		//		c.resumeWithException(RuntimeException("File '$path' doesn't exists"))
		//	}
		//})
	}
}

 */


/*
data class JsStat(val size: Double, var isDirectory: Boolean = false) {
	fun toStat(path: String, vfs: Vfs): VfsStat = vfs.createExistsStat(path, isDirectory = isDirectory, size = size.toLong())
}

class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val stat = fstat(path)
			val handle = open(path, "r")

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val data = read(handle, position.toDouble(), len.toDouble())
					System.arraycopy(data, 0, buffer, offset, data.size)
					return data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = try {
			val stat = fstat(path)
			createExistsStat(path, isDirectory = stat.isDirectory, size = stat.size.toLong())
		} catch (t: Throwable) {
			createNonExistsStat(path)
		}

		suspend override fun list(path: String): AsyncSequence<VfsFile> {
			val emitter = AsyncSequenceEmitter<VfsFile>()
			val fs = jsRequire("fs")
			//console.methods["log"](path)
			fs.call("readdir", path, jsFunctionRaw2 { err, files ->
				//console.methods["log"](err)
				//console.methods["log"](files)
				for (n in 0 until files["length"].toInt()) {
					val file = files[n].toJavaString()
					//println("::$file")
					emitter(file("$path/$file"))
				}
				emitter.close()
			})
			return emitter.toSequence()
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
			val fs = jsRequire("fs")
			val watcher = fs.call("watch", path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
				spawnAndForget(this@withCoroutineContext) {
					val et = eventType.toJavaString()
					val fn = filename.toJavaString()
					val f = file("$path/$fn")
					//println("$et, $fn")
					when (et) {
						"rename" -> {
							val kind = if (f.exists()) VfsFileEvent.Kind.CREATED else VfsFileEvent.Kind.DELETED
							handler(VfsFileEvent(kind, f))
						}
						"change" -> {
							handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, f))
						}
						else -> {
							println("Unhandled event: $et")
						}
					}
				}
			})

			return@withCoroutineContext Closeable { watcher.call("close") }
		}

		override fun toString(): String = "LocalVfs"
	}


	suspend fun open(path: String, mode: String): JsDynamic = korioSuspendCoroutine { c ->
		val fs = jsRequire("fs")
		fs.call("open", path, mode, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.resumeWithException(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				c.resume(fd!!)
			}
		})
	}

	suspend fun read(fd: JsDynamic?, position: Double, len: Double): ByteArray = Promise.create { c ->
		val fs = jsRequire("fs")
		val buffer = jsNew("Buffer", len)
		fs.call("read", fd, buffer, 0, len, position, jsFunctionRaw3 { err, bytesRead, buffer ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening ${fd.toJavaString()}"))
			} else {
				val u8array = jsNew("Int8Array", buffer, 0, bytesRead)
				val out = ByteArray(bytesRead.toInt())
				out.asJsDynamic().call("setArraySlice", 0, u8array)
				c.resolve(out)
			}
		})
	}

	suspend fun close(fd: Any): Unit = Promise.create { c ->
		val fs = jsRequire("fs")
		fs.call("close", fd, jsFunctionRaw2 { err, fd ->
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} closing file"))
			} else {
				c.resolve(Unit)
			}
		})
	}



	suspend fun fstat(path: String): JsStat = Promise.create { c ->
		// https://nodejs.org/api/fs.html#fs_class_fs_stats
		val fs = jsRequire("fs")
		//fs.methods["exists"](path, jsFunctionRaw1 { jsexists ->
		//	val exists = jsexists.toBool()
		//	if (exists) {
		fs.call("stat", path, jsFunctionRaw2 { err, stat ->
			//console.methods["log"](stat)
			if (err != null) {
				c.reject(RuntimeException("Error ${err.toJavaString()} opening $path"))
			} else {
				val out = JsStat(stat["size"].toDouble())
				out.isDirectory = stat.call("isDirectory").toBool()
				c.resolve(out)
			}
		})
		//	} else {
		//		c.resumeWithException(RuntimeException("File '$path' doesn't exists"))
		//	}
		//})
	}
}



class ResourcesVfsProviderJs : ResourcesVfsProvider() {
	override fun invoke(): Vfs {
		return EmbededResourceListing(if (OS.isNodejs) {
			LocalVfs(getCWD())
		} else {
			UrlVfs(getBaseUrl())
		}.jail())
	}

	private fun getCWD(): String = global["process"].call("cwd").toJavaString()

	private fun getBaseUrl(): String {
		var baseHref = document["location"]["href"].call("replace", jsRegExp("/[^\\/]*$"), "")
		val bases = document.call("getElementsByTagName", "base")
		if (bases["length"].toInt() > 0) baseHref = bases[0]["href"]
		return baseHref.toJavaString()
	}
}

@Suppress("unused")
private class EmbededResourceListing(parent: VfsFile) : Vfs.Decorator(parent) {
	val nodeVfs = NodeVfs()

	init {
		for (asset in jsGetAssetStats()) {
			val info = PathInfo(asset.path.trim('/'))
			val folder = nodeVfs.rootNode.access(info.folder, createFolders = true)
			folder.createChild(info.basename, isDirectory = false).data = asset.size
		}
	}

	suspend override fun stat(path: String): VfsStat {
		try {
			val n = nodeVfs.rootNode[path]
			return createExistsStat(path, n.isDirectory, n.data as Long)
		} catch (t: Throwable) {
			return createNonExistsStat(path)
		}
	}

	suspend override fun list(path: String): AsyncSequence<VfsFile> = withCoroutineContext {
		asyncGenerate(this@withCoroutineContext) {
			for (item in nodeVfs.rootNode[path]) {
				yield(file("$path/${item.name}"))
			}
		}
	}

	override fun toString(): String = "ResourcesVfs[$parent]"
}


class LocalVfsProviderJvm : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
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
				asyncGenerate(coroutineContext) {
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

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = withCoroutineContext {
			var running = true
			val fs = FileSystems.getDefault()
			val watcher = fs.newWatchService()

			fs.getPath(path).register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)

			spawnAndForget(this@withCoroutineContext) {
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

			return@withCoroutineContext Closeable {
				running = false
				watcher.close()
			}
		}

		override fun toString(): String = "LocalVfs"
	}
}


class ResourcesVfsProviderJvm : ResourcesVfsProvider() {
	override fun invoke(): Vfs = invoke(ClassLoader.getSystemClassLoader())

	fun invoke(classLoader: ClassLoader): Vfs {
		val merged = MergedVfs()

		return object : Vfs.Decorator(merged.root) {
			suspend override fun init() {
				if (classLoader is URLClassLoader) {
					for (url in classLoader.urLs) {
						val urlStr = url.toString()
						val vfs = if (urlStr.startsWith("http")) {
							UrlVfs(url)
						} else {
							LocalVfs(File(url.toURI()))
						}

						//println(vfs)

						if (vfs.extension in setOf("jar", "zip")) {
							//merged.vfsList += vfs.openAsZip()
						} else {
							merged.vfsList += vfs.jail()
						}
					}
					//println(merged.options)
				}

				//println("ResourcesVfsProviderJvm:classLoader:$classLoader")

				merged.vfsList += object : Vfs() {
					private fun normalize(path: String): String = path.trim('/')

					private fun getResourceAsStream(npath: String) = classLoader.getResourceAsStream(npath) ?: invalidOp("Can't find '$npath' in ResourcesVfsProviderJvm")

					suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = executeInWorker {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:open: $path")
						MemorySyncStream(getResourceAsStream(npath).readBytes()).toAsync()
					}

					suspend override fun stat(path: String): VfsStat = executeInWorker {
						val npath = normalize(path)
						//println("ResourcesVfsProviderJvm:stat: $npath")
						try {
							val s = getResourceAsStream(npath)
							val size = s.available()
							s.read()
							createExistsStat(npath, isDirectory = false, size = size.toLong())
						} catch (e: Throwable) {
							//e.printStackTrace()
							createNonExistsStat(npath)
						}
					}
				}.root
			}

			override fun toString(): String = "ResourcesVfs"
		}
	}
}


 */