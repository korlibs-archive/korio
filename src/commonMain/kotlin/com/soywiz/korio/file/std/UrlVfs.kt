package com.soywiz.korio.file.std

import com.soywiz.kds.*
import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*
import com.soywiz.korio.net.http.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*

fun UrlVfs(url: String, client: HttpClient = createHttpClient()): VfsFile = UrlVfs(URI(url), client)

fun UrlVfs(url: URI, client: HttpClient = createHttpClient()): VfsFile =
	UrlVfs(url.copy(path = "", query = null).fullUri, Unit, client)[url.path]

fun UrlVfsJailed(url: String, client: HttpClient = createHttpClient()): VfsFile = UrlVfsJailed(URI(url), client)

fun UrlVfsJailed(url: URI, client: HttpClient = createHttpClient()): VfsFile =
	UrlVfs(url.fullUri, Unit, client)[url.path]

class UrlVfs(val url: String, val dummy: Unit, val client: HttpClient = createHttpClient()) : Vfs() {
	override val absolutePath: String = url

	fun getFullUrl(path: String): String {
		val result = url.trim('/') + '/' + path.trim('/')
		//println("UrlVfs.getFullUrl: url=$url, path=$path, result=$result")
		return result
	}

	//suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
	//	return if (mode.write) {
	//		TODO()
	//	} else {
	//		client.request(HttpClient.Method.GET, getFullUrl(path)).content.toAsyncStream()
	//	}
	//}

	override suspend fun open(path: String, mode: VfsOpenMode): AsyncStream {
		val fullUrl = getFullUrl(path)

		val stat = stat(path)
		val response = stat.extraInfo as? HttpClient.Response

		if (!stat.exists) {
			throw com.soywiz.korio.FileNotFoundException("Unexistant $fullUrl : $response")
		}

		return object : AsyncStreamBase() {
			override suspend fun read(position: Long, buffer: ByteArray, offset: Int, len: Int): Int {
				if (len == 0) return 0
				val res = client.request(
					Http.Method.GET,
					fullUrl,
					Http.Headers(linkedMapOf("range" to "bytes=$position-${position + len - 1}"))
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

			override suspend fun getLength(): Long = stat.size
		}.toAsyncStream().buffered()
		//}.toAsyncStream()
	}

	override suspend fun openInputStream(path: String): AsyncInputStream {
		return client.request(Http.Method.GET, getFullUrl(path)).content
	}

	override suspend fun readRange(path: String, range: LongRange): ByteArray = client.requestAsBytes(
		Http.Method.GET,
		getFullUrl(path),
		Http.Headers(if (range == LONG_ZERO_TO_MAX_RANGE) LinkedHashMap() else linkedHashMapOf("range" to "bytes=${range.start}-${range.endInclusive}"))
	).content

	class HttpHeaders(val headers: Http.Headers) : Attribute

	override suspend fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		if (content !is AsyncStream) invalidOp("UrlVfs.put requires content to be AsyncStream")
		val headers = attributes.get<HttpHeaders>()
		val mimeType = attributes.get<MimeType>() ?: MimeType.APPLICATION_JSON
		val hheaders = headers?.headers ?: Http.Headers()
		val contentLength = content.getLength()

		client.request(
			Http.Method.PUT, getFullUrl(path), hheaders.withReplaceHeaders(
				"content-length" to "$contentLength",
				"content-type" to mimeType.mime
			), content
		)

		return content.getLength()
	}

	override suspend fun stat(path: String): VfsStat {
		val result = client.request(Http.Method.HEAD, getFullUrl(path))

		return if (result.success) {
			createExistsStat(
				path,
				isDirectory = true,
				size = result.headers["content-length"]?.toLongOrNull() ?: 0L,
				extraInfo = result
			)
		} else {
			createNonExistsStat(path, extraInfo = result)
		}
	}

	override fun toString(): String = "UrlVfs"
}
