package com.soywiz.korio.vfs

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.coroutines.suspendCoroutine

fun LocalVfs(base: String): VfsFile = LocalVfs()[base]
fun TempVfs() = LocalVfs()[System.getProperty("java.io.tmpdir")]
fun LocalVfs(base: File): VfsFile = LocalVfs()[base.absolutePath]
fun JailedLocalVfs(base: File): VfsFile = LocalVfs()[base.absolutePath].jail()
suspend fun File.open(mode: VfsOpenMode) = LocalVfs(this).open(mode)

fun LocalVfs(): VfsFile = _LocalVfs().root

@JTranscMethodBody(target = "js", value = "return {% CONSTRUCTOR com.soywiz.korio.vfs.LocalVfsJs:()V %}();")
private fun _LocalVfs(): Vfs = LocalVfsNio()

@Suppress("unused")
private class LocalVfsJs : Vfs() {
	override fun toString(): String = "LocalVfs"
}

private class LocalVfsNio : Vfs() {
	val baseAbsolutePath = ""
	override val absolutePath: String = baseAbsolutePath

	fun resolve(path: String) = VfsUtil.normalizeAbsolute("$baseAbsolutePath/$path")
	fun resolvePath(path: String) = Paths.get(resolve(path))
	fun resolveFile(path: String) = File(resolve(path))

	suspend override fun exec(path: String, cmdAndArgs: List<String>, handler: VfsProcessHandler): Int = executeInWorker {
		val p = Runtime.getRuntime().exec(cmdAndArgs.toTypedArray(), arrayOf<String>(), resolveFile(path))
		var closing = false
		while (true) {
			val o = p.inputStream.readAvailableChunk(readRest = closing)
			val e = p.errorStream.readAvailableChunk(readRest = closing)
			if (o.isNotEmpty()) handler.onOut(o)
			if (e.isNotEmpty()) handler.onErr(e)
			if (closing) break
			if (o.isEmpty() && e.isEmpty() && !p.isAlive) {
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

		return object : AsyncStream() {
			var position = 0L

			suspend override fun read(buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
				val bb = ByteBuffer.wrap(buffer, offset, len)
				val read = completionHandler<Int> { channel.read(bb, position, Unit, it) }
				position += read
				read
			}

			suspend override fun write(buffer: ByteArray, offset: Int, len: Int) = asyncFun {
				val bb = ByteBuffer.wrap(buffer, offset, len)
				val write = completionHandler<Int> { channel.write(bb, position, Unit, it) }
				position += write
				Unit
			}

			suspend override fun setPosition(value: Long) = run { position = value }
			suspend override fun getPosition(): Long = position
			suspend override fun setLength(value: Long) {
				channel.truncate(value)
			}

			suspend override fun getLength(): Long = channel.size()

			suspend override fun close() {
				channel.close()
			}

			override fun toString(): String = "${this@LocalVfsNio}($path)"
		}
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

	suspend override fun list(path: String) = executeInWorker {
		asyncGenerate {
			for (path in Files.newDirectoryStream(resolvePath(path))) {
				val file = path.toFile()
				yield(VfsFile(this@LocalVfsNio, file.absolutePath))
			}
		}
	}

	suspend override fun mkdir(path: String): Boolean = executeInWorker {
		resolveFile(path).mkdir()
	}

	suspend override fun delete(path: String): Boolean = executeInWorker {
		resolveFile(path).delete()
	}

	suspend override fun rename(src: String, dst: String): Boolean = executeInWorker {
		resolveFile(src).renameTo(resolveFile(dst))
	}

	inline suspend fun <T> completionHandler(crossinline callback: (CompletionHandler<T, Unit>) -> Unit) = suspendCoroutine<T> { c ->
		callback(object : CompletionHandler<T, Unit> {
			override fun completed(result: T, attachment: Unit?) {
				c.resume(result)
			}

			override fun failed(exc: Throwable, attachment: Unit?) {
				c.resumeWithException(exc)
			}
		})
	}

	override fun toString(): String = "LocalVfs"
}
