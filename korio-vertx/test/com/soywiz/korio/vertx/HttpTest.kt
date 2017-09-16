package com.soywiz.korio.vertx

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.http.createHttpServer
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.use
import org.junit.Assert
import org.junit.Test

class HttpTest {
	@Test
	fun testClient() = syncTest {
		val server = HttpServer()
		server.listen { req ->
			var content = ""
			req.handler {
				content += it.toString(Charsets.UTF_8)
			}
			req.endHandler {
				req.end("hello ${req.method} ${req.path} : '$content'")
			}
		}
		val port = server.actualPort

		println("actualPort: $port")
		val client = HttpClient()

		val result = client.request(Http.Method.GET, "http://127.0.0.1:$port/test").readAllBytes().toString(Charsets.UTF_8)
		Assert.assertEquals("hello GET /test : ''", result)

		val result2 = client.request(Http.Method.PUT, "http://127.0.0.1:$port/test", content = "fromclient".openAsync()).readAllBytes().toString(Charsets.UTF_8)
		Assert.assertEquals("hello PUT /test : 'fromclient'", result2)

		server.close()
	}

	@Test
	fun testServer() = syncTest {
		createHttpServer().listen(0) {
			println("Request")
		}.use {

		}
	}
}