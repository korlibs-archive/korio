package com.soywiz.korio.vfs.js

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.buffered
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.AsyncCache
import com.soywiz.korio.vfs.UrlVfsProvider
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat

class UrlVfsProviderJs : UrlVfsProvider {
	override fun invoke(): Vfs = object : Vfs() {
		val statCache = AsyncCache()

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
			var info: VfsStat? = null

			object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
					val res = JsUtils.readRangeBytes(path, position.toDouble(), (position + len - 1).toDouble())
					System.arraycopy(res, 0, buffer, offset, res.size)
					res.size
				}

				suspend override fun getLength(): Long = asyncFun {
					if (info == null) info = stat(path)
					info!!.size
				}
			}.toAsyncStream().buffered()
		}

		//suspend override fun readFully(path: String): ByteArray = JsUtils.readBytes(path)

		suspend override fun stat(path: String): VfsStat = statCache(path) {
			try {
				JsUtils.stat(path).toStat(path, this)
			} catch (e: Throwable) {
				createNonExistsStat(path)
			}
		}
	}
}

