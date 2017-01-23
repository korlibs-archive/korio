package com.soywiz.korio.vfs.jvm

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
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UrlVfsProviderJvm : UrlVfsProvider {
	override fun invoke(): Vfs = object : Vfs() {
		val statCache = AsyncCache()

		suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
			var info: VfsStat? = null

			return object : AsyncStreamBase() {
				suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = executeInWorker {
					val conn = URL(path).openConnection() as HttpURLConnection
					conn.setRequestProperty("Range", "bytes=$position-${(position + len) - 1}")
					conn.connect()
					val bis = BufferedInputStream(conn.inputStream)
					var cpos = offset
					var pending = len
					var totalRead = 0
					while (pending > 0) {
						checkCancelled()
						val read = bis.read(buffer, cpos, Math.max(pending, 0x4000))
						if (read == 0) break
						if (read <= 0) throw IOException("Read: $read")
						cpos += read
						pending -= read
						totalRead += read
					}
					totalRead
				}

				suspend override fun getLength(): Long {
					if (info == null) info = stat(path)
					return info!!.size
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

