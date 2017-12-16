package com.soywiz.korio.vfs

import com.soywiz.kds.lmapOf
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.URI
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.LONG_ZERO_TO_MAX_RANGE

fun UrlVfs(url: String): VfsFile = UrlVfs(URI(url))
fun UrlVfs(url: String, client: HttpClient): VfsFile = UrlVfs(URI(url), client)
fun UrlVfs(url: URI, client: HttpClient = createHttpClient()): VfsFile = UrlVfs(url.copy(path = "", query = null).fullUri, Unit, client)[url.path]

class UrlVfs(val url: String, val dummy: Unit, val client: HttpClient = createHttpClient()) : Vfs() {
	override val absolutePath: String = url

	fun getFullUrl(path: String) = url.trim('/') + '/' + path.trim('/')

	//suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
	//	return if (mode.write) {
	//		TODO()
	//	} else {
	//		client.request(HttpClient.Method.GET, getFullUrl(path)).content.toAsyncStream()
	//	}
	//}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val fullUrl = getFullUrl(path)

		val stat = stat(path)
		val response = stat.extraInfo as? HttpClient.Response

		if (!stat.exists) {
			throw com.soywiz.korio.FileNotFoundException("Unexistant $fullUrl : $response")
		}

		return object : AsyncStreamBase() {
			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				if (len == 0) return 0
				val res = client.request(
					Http.Method.GET,
					fullUrl,
					Http.Headers(lmapOf("range" to "bytes=$position-${position + len - 1}"))
				)
				val s = res.content
				var coffset = offset
				var pending = len
				var totalRead = 0
				while (pending > 0) {
					val read = s.read(buffer, coffset, pending)
					if (read < 0 && totalRead == 0) return read
					if (read <= 0) break
					pending -= read
					totalRead += read
					coffset += read
				}
				return totalRead
			}

			suspend override fun getLength(): Long = stat.size
		}.toAsyncStream().buffered()
		//}.toAsyncStream()
	}

	suspend override fun openInputStream(path: String): AsyncInputStream {
		return client.request(Http.Method.GET, getFullUrl(path)).content
	}

	suspend override fun readRange(path: String, range: LongRange): ByteArray = client.requestAsBytes(
		Http.Method.GET,
		getFullUrl(path),
		Http.Headers(if (range == LONG_ZERO_TO_MAX_RANGE) lmapOf() else lmapOf("range" to "bytes=${range.start}-${range.endInclusive}"))
	).content

	class HttpHeaders(val headers: Http.Headers) : Attribute

	suspend override fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		if (content !is AsyncStream) invalidOp("UrlVfs.put requires content to be AsyncStream")
		val headers = attributes.get<HttpHeaders>()
		val mimeType = attributes.get<MimeType>() ?: MimeType.APPLICATION_JSON
		val hheaders = headers?.headers ?: Http.Headers()
		val contentLength = content.getLength()

		client.request(Http.Method.PUT, getFullUrl(path), hheaders.withReplaceHeaders(
			"content-length" to "$contentLength",
			"content-type" to mimeType.mime
		), content)

		return content.getLength()
	}

	suspend override fun stat(path: String): VfsStat {
		val result = client.request(Http.Method.HEAD, getFullUrl(path))

		return if (result.success) {
			createExistsStat(path, isDirectory = true, size = result.headers["content-length"]?.toLongOrNull() ?: 0L, extraInfo = result)
		} else {
			createNonExistsStat(path, extraInfo = result)
		}
	}
}
