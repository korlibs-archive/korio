package com.soywiz.korio.ext.web.router

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.FakeRequest
import com.soywiz.korio.net.http.Http
import org.junit.Assert
import org.junit.Test

class KorRouterTest {
	val injector = AsyncInjector()
	val router = KorRouter(injector)

	suspend private fun KorRouter.testRoute(method: Http.Method, uri: String, headers: Http.Headers = Http.Headers(), body: ByteArray = ByteArray(0)): String {
		val request = FakeRequest(method, uri, headers, body)
		router.accept(request)
		return request.toString()
	}

	@Test
	fun testSimpleRoute() = syncTest {
		@Suppress("unused")
		class DemoRoute {
			@Route(Http.Methods.GET, "/hello/:name")
			fun test(@Param("name") name: String): String {
				return "hello $name"
			}

			@Route(Http.Methods.GET, "/hellotext/:name", textContentType = "text/plain")
			fun test2(@Param("name") name: String): String {
				return "hellotext $name"
			}

			@Route(Http.Methods.GET, "/api/test")
			fun test2() = mapOf("hello" to "world")
		}

		Assert.assertEquals(
				"200:OK:Headers((Content-Length, [11]), (Content-Type, [text/html])):hello world",
				router.registerRoutes<DemoRoute>().testRoute(Http.Method.GET, "/hello/world")
		)

		Assert.assertEquals(
				"200:OK:Headers((Content-Length, [15]), (Content-Type, [text/plain])):hellotext world",
				router.registerRoutes<DemoRoute>().testRoute(Http.Method.GET, "/hellotext/world")
		)

		Assert.assertEquals(
				"""200:OK:Headers((Content-Length, [17]), (Content-Type, [application/json])):{"hello":"world"}""",
				router.registerRoutes<DemoRoute>().testRoute(Http.Method.GET, "/api/test")
		)
	}
}