package com.soywiz.korio.vfs.android

import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.isAliveJre7
import com.soywiz.korio.vfs.*
import java.io.*

// Do not use NIO since it is not available on android!
class LocalVfsProviderAndroid : LocalVfsProvider {
	override fun invoke(): Vfs = object : Vfs() {
		val that = this
		val baseAbsolutePath = ""
		override val absolutePath: String = baseAbsolutePath

		fun resolve(path: String) = VfsUtil.lightCombine(baseAbsolutePath, path)
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

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = executeInWorker {
			val raf = RandomAccessFile(path, mode.cmode)
			if (mode.truncate) raf.setLength(0L)

			object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
					synchronized(raf) {
						raf.seek(position)
						raf.read(buffer, offset, len)
					}
				}

				suspend override fun write(position: Long, buffer: ByteArray, offset: Int, len: Int) = executeInWorker {
					synchronized(raf) {
						raf.seek(position)
						raf.write(buffer, offset, len)
					}
					Unit
				}

				suspend override fun setLength(value: Long): Unit = executeInWorker {
					synchronized(raf) {
						raf.setLength(value); Unit
					}
				}

				suspend override fun getLength(): Long = raf.length()
				suspend override fun close() = raf.close()

				override fun toString(): String = "$that($path)"
			}.toAsyncStream()
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
				for (file in File(path).listFiles() ?: arrayOf()) {
					yield(that.file("$path/${file.name}"))
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

		override fun toString(): String = "LocalVfs"
	}
}

