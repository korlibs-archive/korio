@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package com.soywiz.korio.vfs.js

import com.jtransc.js.jsFunctionRaw2
import com.jtransc.js.methods
import com.jtransc.js.toJavaString
import com.jtransc.js.toJavaStringOrNull
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.sleep
import com.soywiz.korio.async.spawn
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.*
import java.io.Closeable

class LocalVfsProviderJs : LocalVfsProvider() {
	override fun invoke(): Vfs = object : Vfs() {
		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
			val stat = NodeJsUtils.fstat(path)
			val handle = NodeJsUtils.open(path, "r")

			object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
					val data = NodeJsUtils.read(handle, position.toDouble(), len.toDouble())
					System.arraycopy(data, 0, buffer, offset, data.size)
					data.size
				}

				suspend override fun getLength(): Long = stat.size.toLong()
				suspend override fun close(): Unit = NodeJsUtils.close(handle)
			}.toAsyncStream()
		}

		suspend override fun stat(path: String): VfsStat = asyncFun {
			try {
				val stat = NodeJsUtils.fstat(path)
				createExistsStat(path, isDirectory = stat.isDirectory, size = stat.size.toLong())
			} catch (t: Throwable) {
				createNonExistsStat(path)
			}
		}

		suspend override fun watch(path: String, handler: (VfsFileEvent) -> Unit): Closeable = asyncFun {
			val fs = jsRequire("fs")
			val watcher = fs.methods["watch"](path, jsObject("persistent" to true, "recursive" to true), jsFunctionRaw2 { eventType, filename ->
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

			Closeable {
				watcher.methods["close"]()
			}
		}

		override fun toString(): String = "LocalVfs"
	}
}
