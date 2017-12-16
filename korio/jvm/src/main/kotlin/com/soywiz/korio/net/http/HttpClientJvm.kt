package com.soywiz.korio.net.http

import com.soywiz.klock.Klock
import com.soywiz.korio.async.*
import com.soywiz.korio.coroutine.currentThreadId
import com.soywiz.korio.coroutine.eventLoop
import com.soywiz.korio.coroutine.getCoroutineContext
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.slice
import com.soywiz.korio.stream.withLength
import com.soywiz.korio.util.toUintClamp
import java.net.BindException
import java.net.HttpURLConnection
import java.net.URL

class HttpClientJvm : HttpClient() {
	companion object {
		var lastId = 0
	}

	val clientId = lastId++
	var lastRequestId = 0

	suspend override fun requestInternal(method: Http.Method, url: String, headers: Http.Headers, content: AsyncStream?): Response {
		val result = executeInWorker {
			val requestId = lastRequestId++
			val id = "request[$clientId,$requestId]"

			//println("url[$id] thread=$currentThreadId: $url ")
			val aurl = URL(url)
			HttpURLConnection.setFollowRedirects(false)
			val con = aurl.openConnection() as HttpURLConnection
			con.requestMethod = method.name
			//println(" --> [$id]${method.name}")
			//println("URL:$url")
			//println("METHOD:${method.name}")
			for (header in headers) {
				//println("HEADER:$header")
				con.addRequestProperty(header.first, header.second)
				//println(" --> [$id]${header.first} - ${header.second}")
			}
			//println(" --> [$id]content=${content?.size()}")
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
						getCoroutineContext().eventLoop.sleep(1000)
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

			spawnAndForget {
				executeInNewThread {
					val syncStream = ignoreErrors { con.inputStream } ?: ignoreErrors { con.errorStream }
					try {
						if (syncStream != null) {
							//val stream = syncStream.toAsync(length).toAsyncStream()
							val stream = syncStream
							val temp = ByteArray(0x1000)
							loop@ while (true) {
								// @TODO: Totally cancel reading if nobody is consuming this. Think about the best way of doing this.
								// node.js pause equivalent?
								val chunkStartTime = Klock.currentTimeMillis()
								while (produceConsumer.availableCount > 4) { // Prevent filling the memory if nobody is consuming data
									//println("PREVENT!")
									eventLoop().sleep(10)
									val chunkCurrentTime = Klock.currentTimeMillis()
									if ((chunkCurrentTime - chunkStartTime) >= 2000L) {
										System.err.println("[$id] thread=$currentThreadId Two seconds passed without anyone reading data (available=${produceConsumer.availableCount}) from $url. Closing...")
										break@loop
									}
								}
								val read = stream.read(temp)
								//println(" --- [$id][D] thread=$currentThreadId : $read")
								if (read <= 0) break
								produceConsumer.produce(temp.copyOf(read))
							}
						}
					} finally {
						ignoreErrors { syncStream?.close() }
						ignoreErrors { produceConsumer.close() }
						ignoreErrors { con.disconnect() }
						HttpStats.disconnections.incrementAndGet()
					}
				}
			}

			//Response(
			//		status = con.responseCode,
			//		statusText = con.responseMessage,
			//		headers = Http.Headers.fromListMap(con.headerFields),
			//		content = if (con.responseCode < 400) con.inputStream.readBytes().openAsync() else con.errorStream.toAsync().toAsyncStream()
			//)

			val acontent = produceConsumer.toAsyncInputStream()
			Response(
				status = con.responseCode,
				statusText = con.responseMessage,
				headers = pheaders,
				content = if (length != null) acontent.withLength(length) else acontent
			)
		}
		return result
	}
}