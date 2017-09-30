package com.soywiz.korio.net.http

import com.soywiz.korio.async.*
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.toUintClamp
import java.io.FileNotFoundException
import java.net.BindException
import java.net.HttpURLConnection
import java.net.URL

actual object DefaultHttpFactoryFactory {
	actual fun createFactory(): HttpFactory = object : HttpFactory {
		init {
			System.setProperty("http.keepAlive", "false")
		}

		override fun createClient(): HttpClient = HttpClientJvm()

		override fun createServer(): HttpServer = TODO()
	}
}

class HttpClientJvm : HttpClient() {
	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response = executeInWorker {
		try {
			//println("url: $url")
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

				val ccontent = content.slice()
				val len = ccontent.getAvailable()
				var left = len
				val temp = ByteArray(1024)
				//println("HEADER:content-length, $len")

				while (true) {
					try {
						con.connect()
						HttpStats.connections.incrementAndGet()
						break
					} catch (e: BindException) {
						// Potentially no more ports available. Too many pending connections.
						e.printStackTrace()
						coroutineContext.eventLoop.sleep(1000)
						continue
					}
				}

				val os = con.outputStream
				while (left > 0) {
					val read = ccontent.read(temp, 0, Math.min(temp.size, left.toUintClamp()))
					if (read <= 0) invalidOp("Problem reading")
					left -= read
					os.write(temp, 0, read)
				}
				os.flush()
				os.close()
			} else {
				con.connect()
			}

			val produceConsumer = ProduceConsumer<ByteArray>()

			val pheaders = Http.Headers.fromListMap(con.headerFields)
			val length = pheaders["Content-Length"]?.toLongOrNull()

			spawnAndForget(coroutineContext) {
				val syncStream = if (con.responseCode < 400) con.inputStream else con.errorStream
				try {
					if (syncStream != null) {
						val stream = syncStream.toAsync(length).toAsyncStream()
						val temp = ByteArray(0x1000)
						while (true) {
							// @TODO: Totally cancel reading if nobody is consuming this. Think about the best way of doing this.
							// node.js pause equivalent?
							while (produceConsumer.availableCount > 4) { // Prevent filling the memory if nobody is consuming data
								coroutineContext.eventLoop.sleep(100)
							}
							val read = stream.read(temp)
							if (read <= 0) break
							produceConsumer.produce(temp.copyOf(read))
						}
					}
				} finally {
					ignoreErrors { syncStream.close() }
					ignoreErrors { produceConsumer.close() }
					ignoreErrors { con.disconnect() }
					HttpStats.disconnections.incrementAndGet()
				}
			}

			//Response(
			//		status = con.responseCode,
			//		statusText = con.responseMessage,
			//		headers = Http.Headers.fromListMap(con.headerFields),
			//		content = if (con.responseCode < 400) con.inputStream.readBytes().openAsync() else con.errorStream.toAsync().toAsyncStream()
			//)

			Response(
				status = con.responseCode,
				statusText = con.responseMessage,
				headers = pheaders,
				content = produceConsumer.toAsyncInputStream()
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