package com.soywiz.korio.vertx

import com.soywiz.korio.async.AsyncQueue
import com.soywiz.korio.async.ProduceConsumer
import com.soywiz.korio.async.Promise
import com.soywiz.korio.async.toAsyncInputStream
import com.soywiz.korio.ds.MapList
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpFactory
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.readBytes
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import java.net.URL

class VertxHttpClientFactory : HttpFactory() {
	override val available: Boolean = false
	override val priority: Int = 500

	//override fun create(): HttpClient = VertxHttpClient()
	override fun createClient(): HttpClient = TODO()
}

/*
class VertxHttpClient : HttpClient() {
	val vxMethods = HttpMethod.values().map { it.name to it }.toMap()
	val krMethods = HttpClient.Method.values().map { it.name to it }.toMap()
	val convertMethod = krMethods.values.map { it to vxMethods[it.name] }.toMap()

	suspend override fun request(method: Method, url: String, headers: Headers, content: AsyncStream?): Response {
		val urlUrl = URL(url)
		val ssl = when (urlUrl.protocol) {
			"https", "wss" -> true
			else -> false
		}
		val vxMethod = convertMethod[method]!!
		val vxclient = vertx.createHttpClient(HttpClientOptions().setSsl(ssl))
		val req = vxclient.requestAbs(vxMethod, url)
		for ((k, v) in headers) req.putHeader(k, v)
		if (content != null) {
			while (!content.eof()) {
				val data = content.readBytes(0x1000)
				req.write(Buffer.buffer(data))
			}
		}

		val p = ProduceConsumer<ByteArray>()

		val deferred = Promise.Deferred<HttpClientResponse>()

		req.handler { res ->
			res.handler { p.produce(it.bytes) }
			res.endHandler { deferred.resolve(res) }
		}

		req.end()

		val res = deferred.promise.await()

		return Response(
				status = res.statusCode(),
				statusText = res.statusMessage(),
				headers = HttpClient.Headers(MapList(res.headers().map { it.key to it.value })),
				content = p.toAsyncInputStream()
		)
	}
}
*/