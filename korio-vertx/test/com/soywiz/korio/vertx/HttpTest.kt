package com.soywiz.korio.vertx

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpServer
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.use
import com.soywiz.korio.vertx.router.Header
import com.soywiz.korio.vertx.router.Post
import com.soywiz.korio.vertx.router.Route
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import org.junit.Assert
import org.junit.Test

class HttpTest {
	@Test
	fun testClient() = syncTest {
		val server = vx<HttpServer> {
			vertx.createHttpServer().requestHandler { req ->
				var content = ""
				req.handler {
					content += it
				}
				req.endHandler {
					val res = req.response()
					res.end("hello ${req.method()} ${req.path()} : '$content'")
				}
			}.listen(0, it)
		}
		val port = server.actualPort()

		val client = HttpClient()

		val result = client.request(Http.Method.GET, "http://127.0.0.1:$port/test").readAllBytes().toString(Charsets.UTF_8)
		Assert.assertEquals("hello GET /test : ''", result)

		val result2 = client.request(Http.Method.PUT, "http://127.0.0.1:$port/test", content = "fromclient".openAsync()).readAllBytes().toString(Charsets.UTF_8)
		Assert.assertEquals("hello PUT /test : 'fromclient'", result2)

		vx<Void> { server.close(it) }
	}

	@Test
	fun testServer() = syncTest {
		createHttpServer().listen(0) {
			println("Request")
		}.use {

		}
	}
}