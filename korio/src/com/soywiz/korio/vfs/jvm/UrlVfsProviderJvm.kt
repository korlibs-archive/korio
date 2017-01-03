package com.soywiz.korio.vfs.jvm

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.buffered
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.AsyncCache
import com.soywiz.korio.vfs.UrlVfsProvider
import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsOpenMode
import com.soywiz.korio.vfs.VfsStat
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL

class UrlVfsProviderJvm : UrlVfsProvider {
	override fun invoke(): Vfs = object : Vfs() {
		val statCache = AsyncCache()

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
			var info: VfsStat? = null

			object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
					val conn = URL(path).openConnection() as HttpURLConnection
					conn.setRequestProperty("Range", "bytes=$position-${(position + len) - 1}")
					conn.connect()
					val res = BufferedInputStream(conn.inputStream).readBytes()
					System.arraycopy(res, 0, buffer, offset, res.size)
					res.size
				}

				suspend override fun getLength(): Long = asyncFun {
					if (info == null) info = stat(path)
					info!!.size
				}
			}.toAsyncStream().buffered()
		}

		//suspend override fun readFully(path: String): ByteArray = executeInWorker {
		//	val s = URL(path).openStream()
		//	s.readBytes()
		//}

		suspend override fun stat(path: String): VfsStat = statCache(path) {
			executeInWorker {
				val s = URL(path)
				try {
					HttpURLConnection.setFollowRedirects(false)
					val con = (s.openConnection() as HttpURLConnection).apply {
						requestMethod = "HEAD"
					}
					if (con.responseCode != HttpURLConnection.HTTP_OK) throw RuntimeException("Http error")
					createExistsStat(path, isDirectory = true, size = con.getHeaderField("Content-Length").toLongOrNull() ?: 0L)
				} catch (e: Exception) {
					createNonExistsStat(path)
				}
			}
		}
	}
}

