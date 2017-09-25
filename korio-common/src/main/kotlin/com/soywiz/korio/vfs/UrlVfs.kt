package com.soywiz.korio.vfs

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.LONG_ZERO_TO_MAX_RANGE

fun UrlVfs(url: String): VfsFile = UrlVfs(url, Unit).root

class UrlVfs(val url: String, val dummy: Unit) : Vfs() {
	override val absolutePath: String = url
	val client = createHttpClient()

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
			throw FileNotFoundException("Unexistant $fullUrl : $response")
		}

		return object : AsyncStreamBase() {
			suspend override fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				if (len == 0) return 0
				val res = client.request(
					Http.Method.GET,
					fullUrl,
					Http.Headers(lmapOf("range" to "bytes=$position-${position + len - 1}"))
				)
				val out = res.content.read(buffer, offset, len)
				return out
			}

			suspend override fun getLength(): Long = stat.size
			//suspend override fun getLength(): Long = 0L
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
