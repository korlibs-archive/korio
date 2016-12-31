package com.soywiz.korio.vfs

import com.jtransc.annotation.JTranscMethodBody
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.buffered
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.AsyncCache
import com.soywiz.korio.vfs.js.JsUtils
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL


fun UrlVfs(url: String): VfsFile = _UrlVfs()[url]
fun UrlVfs(url: URL): VfsFile = _UrlVfs()[url.toString()]

@JTranscMethodBody(target = "js", value = "return {% CONSTRUCTOR com.soywiz.korio.vfs.UrlVfsJs:()V %}();")
private fun _UrlVfs(): Vfs = UrlVfsJvm()

@Suppress("unused")
internal class UrlVfsJs : Vfs() {
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

	suspend override fun readFully(path: String): ByteArray = JsUtils.readBytes(path)

	suspend override fun stat(path: String): VfsStat = statCache(path) {
		try {
			val info = JsUtils.stat(path)
			createExistsStat(path, isDirectory = false, size = info.size.toLong())
		} catch (e: Throwable) {
			createNonExistsStat(path)
		}
	}
}

internal class UrlVfsJvm : Vfs() {
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

	suspend override fun readFully(path: String): ByteArray = executeInWorker {
		val s = URL(path).openStream()
		s.readBytes()
	}

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