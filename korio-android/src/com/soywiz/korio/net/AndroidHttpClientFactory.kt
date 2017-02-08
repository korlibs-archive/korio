package com.soywiz.korio.net

import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsync
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.toUintClamp
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

class AndroidHttpClientFactory : HttpFactory() {
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
					val os = con.outputStream
					while (left > 0) {
						val read = content.read(temp, 0, Math.min(temp.size, left.toUintClamp()))
						if (read <= 0) invalidOp("Problem reading")
						left -= read
						os.write(temp, 0, read)
					}
					os.flush()
					os.close()
				}

				con.connect()

				Response(
						status = con.responseCode,
						statusText = con.responseMessage,
						headers = Http.Headers.fromListMap(con.headerFields),
						content = if (con.responseCode < 400) con.inputStream.toAsync().toAsyncStream() else con.errorStream.toAsync().toAsyncStream()
				)
			} catch (e: FileNotFoundException) {
				Response(
						status = 404,
						statusText = "NotFound ${e.message}",
						headers = Http.Headers(),
						content = byteArrayOf().openAsync()
				)
			}
		}
	}
}