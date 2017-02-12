package com.soywiz.korio.vertx

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import io.vertx.core.http.HttpServer
import org.junit.Assert
import org.junit.Test

class HttpTest {
	@Test
	fun testClient() = syncTest {
		val server = vx<HttpServer> {
			vertx.createHttpServer().requestHandler { req ->
				val res = req.response()
				res.end("hello ${req.path()}")
			}.listen(0, it)
		}
		val port = server.actualPort()

		val client = HttpClient()

		val result = client.request(Http.Method.GET, "http://127.0.0.1:$port/test").readAllBytes().toString(Charsets.UTF_8)

		Assert.assertEquals("hello /test", result)

		vx<Void> { server.close(it) }
	}
}