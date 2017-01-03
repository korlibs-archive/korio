package com.soywiz.korio.vfs.js

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.LocalVfsProvider
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat

class LocalVfsProviderJs : LocalVfsProvider {
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

		override fun toString(): String = "LocalVfs"
	}
}
