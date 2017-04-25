@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.android

import android.os.Environment
import android.os.FileObserver
import com.soywiz.korio.android.KorioAndroidContext
import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.asyncGenerate
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.async.sleep
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.isAliveJre7
import com.soywiz.korio.vfs.*
import java.io.*

// Do not use NIO since it is not available on android!
class LocalVfsProviderAndroid : LocalVfsProvider() {
	override fun invoke(): LocalVfs = object : LocalVfs() {
		val that = this
		override val absolutePath: String = ""

		fun resolve(path: String) = VfsUtil.lightCombine("", path)
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

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = executeInWorker {
			//ActivityCompat
			var raf: RandomAccessFile
			while (true) {
				try {
					raf = RandomAccessFile(path, mode.cmode)
					break
				} catch (e: Throwable) {
					if (e.message?.contains("Permission denied", ignoreCase = true) ?: true) {
						KorioAndroidContext.requestPermission("android.permission.WRITE_EXTERNAL_STORAGE")
						sleep(5000)
					} else {
						throw e
					}
				}
			}
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

		suspend override fun setSize(path: String, size: Long) = executeInWorker {
			RandomAccessFile(resolveFile(path), "rw").use { it.setLength(size) }
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
			var movedFrom: String? = null
			var movedTo: String? = null
			val observer = object : FileObserver(resolveFile(path).absolutePath, FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_FROM or FileObserver.MOVED_TO) {
				override fun onEvent(event: Int, cpath: String) {
					val rpath = "$path/$cpath"
					EventLoop.queue {
						when (event) {
							FileObserver.CREATE, FileObserver.MOVED_TO -> handler(VfsFileEvent(VfsFileEvent.Kind.CREATED, that[rpath]))
							FileObserver.MODIFY -> handler(VfsFileEvent(VfsFileEvent.Kind.MODIFIED, that[rpath]))
							FileObserver.DELETE, FileObserver.MOVED_FROM -> handler(VfsFileEvent(VfsFileEvent.Kind.DELETED, that[rpath]))
						//FileObserver.MOVED_FROM, FileObserver.MOVED_TO -> {
						//	if (event == FileObserver.MOVED_FROM) movedFrom = rpath
						//	//if (event == FileObserver.MOVED_FROM) movedFrom = rpath
						//	//if (event == FileObserver.MOVED_TO) movedTo = rpath
						//	//if (movedFrom != null && movedTo != null) {
						//	//	// @TODO: This could be wrong!
						//	//	handler(VfsFileEvent(VfsFileEvent.Kind.RENAMED, that[movedFrom!!], that[movedTo!!]))
						//	//}
						//}
						}
					}
				}
			}
			observer.startWatching()
			return Closeable { observer.stopWatching() }
		}

		override fun toString(): String = "LocalVfs"
	}

	override fun getCacheFolder(): String {
		return Environment.getDownloadCacheDirectory().absolutePath
	}

	override fun getExternalStorageFolder(): String {
		return Environment.getExternalStorageDirectory().absolutePath
	}
}