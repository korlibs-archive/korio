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

	@Test
	fun testSimpleRoute() = syncTest {
		@Suppress("unused")
		class DemoRoute {
			@Route(Http.Methods.GET, "/hello/:name")
			fun test(@Param("name") name: String): String {
				return "hello $name"
			}
		}

		router.registerRoutes<DemoRoute>()
		val request = FakeRequest(Http.Method.GET, "/hello/world")
		router.accept(request)
		Assert.assertEquals("200:OK:Headers((Content-Length, [11]), (Content-Type, [text/html])):hello world", request.toString())
	}
}