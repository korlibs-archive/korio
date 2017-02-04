@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.js

import com.jtransc.js.*
import com.soywiz.korio.async.AsyncSequence
import com.soywiz.korio.async.AsyncSequenceEmitter
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.*
import java.io.Closeable

class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): Vfs = object : Vfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			val stat = NodeJsUtils.fstat(path)
			val handle = NodeJsUtils.open(path, "r")

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
					val data = NodeJsUtils.read(handle, position.toDouble(), len.toDouble())
					System.arraycopy(data, 0, buffer, offset, data.size)
					return data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = NodeJsUtils.close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = try {
			val stat = NodeJsUtils.fstat(path)
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

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable {
			val fs = jsRequire("fs")
			val watcher = fs.call("watch", path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
				spawnAndForget {
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

			return Closeable { watcher.call("close") }
		}

		override fun toString(): String = "LocalVfs"
	}
}
