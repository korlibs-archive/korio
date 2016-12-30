package com.soywiz.korio.vfs

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.AsyncStreamBase
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.JsMethodBody
import com.soywiz.korio.util.OS
import com.soywiz.korio.vfs.js.BrowserJsUtils
import com.soywiz.korio.vfs.js.NodeJsUtils
import java.net.HttpURLConnection
import java.net.URL


fun UrlVfs(url: String): VfsFile = _UrlVfs(url).root
fun UrlVfs(url: URL): VfsFile = _UrlVfs(url.toString()).root

@JsMethodBody("return {% CONSTRUCTOR com.soywiz.korio.vfs.UrlVfsJs:(Ljava/lang/String;)V %}(p0);")
private fun _UrlVfs(url: String): Vfs = UrlVfsJvm(url)

@Suppress("unused")
internal class UrlVfsJs(val urlStr: String) : Vfs() {
	fun resolve(path: String) = "$urlStr/$path".trim('/')

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream = asyncFun {
		val info = stat(path)
		val url = resolve(path)

		object : AsyncStreamBase() {
			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int = asyncFun {
				val res = BrowserJsUtils.readRangeBytes(url, position.toDouble(), (position + len).toDouble())
				System.arraycopy(res, 0, buffer, offset, res.size)
				res.size
			}

			suspend override fun getLength(): Long = info.size
		}.toAsyncStream()
	}

	suspend override fun readFully(path: String): ByteArray = asyncFun {
		if (OS.isNodejs) {
			NodeJsUtils.readURLNodeJs(resolve(path))
		} else {
			BrowserJsUtils.readBytes(resolve(path))
		}
	}

	suspend override fun stat(path: String): VfsStat = asyncFun {
		if (OS.isNodejs) {
			TODO("stat on nodejs")
		} else {
			try {
				val info = BrowserJsUtils.statURLBrowser(resolve(path).toString())
				createExistsStat(path, isDirectory = false, size = info.size.toLong())
			} catch (e: Throwable) {
				createNonExistsStat(path)
			}
		}
	}
}

internal class UrlVfsJvm(val urlStr: String) : Vfs() {
	fun resolve(path: String) = URL("$urlStr/$path".trim('/'))

	suspend override fun readFully(path: String): ByteArray = executeInWorker {
		val s = resolve(path).openStream()
		s.readBytes()
	}

	suspend override fun stat(path: String): VfsStat = executeInWorker {
		val s = resolve(path)
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