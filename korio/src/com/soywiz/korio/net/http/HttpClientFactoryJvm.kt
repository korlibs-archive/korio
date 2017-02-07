package com.soywiz.korio.net.http

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.toUintClamp
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

class HttpClientFactoryJvm : HttpFactory() {
	override fun createClient(): HttpClient = object : HttpClient() {
		suspend override fun request(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = executeInWorker {
			try {
				val aurl = URL(url)
				HttpURLConnection.setFollowRedirects(false)
				val con = aurl.openConnection() as HttpURLConnection
				con.requestMethod = method.name
				//println("URL:$url")
				//println("METHOD:${method.name}")
				for (header in headers) {
					//println("HEADER:$header")
					con.addRequestProperty(header.first, header.second)
				}
				if (content != null) {
					con.doOutput = true

					val len = content.getAvailable()
					var left = len
					val temp = ByteArray(1024)
					//println("HEADER:content-length, $len")

					con.connect()

					val os = con.outputStream
					while (left > 0) {
						val read = content.read(temp, 0, Math.min(temp.size, left.toUintClamp()))
						if (read <= 0) invalidOp("Problem reading")
						left -= read
						os.write(temp, 0, read)
					}
					os.flush()
					os.close()
				} else {
					con.connect()
				}

				Response(
						status = con.responseCode,
						statusText = con.responseMessage,
						headers = Http.Headers.fromListMap(con.headerFields),
						content = con.inputStream.toAsync().toAsyncStream()
				)
			} catch (e: FileNotFoundException) {
				Response(
						status = 404,
						statusText = "NotFound",
						headers = Http.Headers(),
						content = byteArrayOf().openAsync()
				)
			}
		}
	}
}

/*
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
*/
