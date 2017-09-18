package com.soywiz.korio.ext.web.router

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.FakeRequest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.vfs.MemoryVfsMix
import com.soywiz.korio.vfs.VfsFile
import org.junit.Assert
import org.junit.Test

class KorRouterTest {
	val injector = AsyncInjector()
	val router = KorRouter(injector)

	suspend private fun KorRouter.testRoute(method: Http.Method, uri: String, headers: Http.Headers = Http.Headers(), body: ByteArray = ByteArray(0)): String {
		val request = FakeRequest(method, uri, headers, body)
		router.accept(request)
		println(request.log.joinToString("\n"))
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

		router.registerRoutes<DemoRoute>()

		Assert.assertEquals(
				"200:OK:Headers((Content-Length, [11]), (Content-Type, [text/html])):hello world",
				router.testRoute(Http.Method.GET, "/hello/world")
		)

		Assert.assertEquals(
				"200:OK:Headers((Content-Length, [15]), (Content-Type, [text/plain])):hellotext world",
				router.testRoute(Http.Method.GET, "/hellotext/world")
		)

		Assert.assertEquals(
				"""200:OK:Headers((Content-Length, [17]), (Content-Type, [application/json])):{"hello":"world"}""",
				router.testRoute(Http.Method.GET, "/api/test")
		)
	}

	@Test
	fun testStaticRoute() = syncTest {
		@Suppress("unused")
		class StaticRoute {
			val files = MemoryVfsMix(
					"robots.txt" to "User-agent: *"
			)

			@Route(Http.Methods.GET, "/*", priority = RoutePriority.LOWEST)
			fun static(req: HttpServer.Request): VfsFile = files[req.path]

			@Route(Http.Methods.HEAD, "/*", priority = RoutePriority.LOWEST)
			fun staticHead(req: HttpServer.Request): VfsFile = files[req.path]
		}

		router.registerRoutes<StaticRoute>()

		Assert.assertEquals(
				"200:OK:Headers((Accept-Ranges, [bytes]), (Content-Length, [13]), (Content-Type, [text/plain])):User-agent: *",
				router.testRoute(Http.Method.GET, "/robots.txt")
		)

		Assert.assertEquals(
			"200:OK:Headers((Accept-Ranges, [bytes]), (Content-Length, [13]), (Content-Type, [text/plain])):User-agent: *",
			router.testRoute(Http.Method.GET, "/robots.txt?v=3.2.0")
		)

		Assert.assertEquals(
				"404:Not Found:Headers((Content-Length, [30]), (Content-Type, [text/html])):404 - Not Found - /donotexists",
				router.testRoute(Http.Method.GET, "/donotexists")
		)

		// Partial content: GET
		Assert.assertEquals(
			"206:Partial Content:Headers((Accept-Ranges, [bytes]), (Content-Length, [2]), (Content-Range, [bytes 1-2/13]), (Content-Type, [text/plain])):se",
			router.testRoute(Http.Method.GET, "/robots.txt", Http.Headers("Range" to "bytes=1-2"))
		)

		// Partial content: HEAD
		Assert.assertEquals(
			"206:Partial Content:Headers((Accept-Ranges, [bytes]), (Content-Length, [0]), (Content-Range, [bytes 1-2/13]), (Content-Type, [text/plain])):",
			router.testRoute(Http.Method.HEAD, "/robots.txt", Http.Headers("Range" to "bytes=1-2"))
		)
	}

	@Test
	fun testRedirect() = syncTest {
		@Suppress("unused")
		class TestRoute {
			@Route(Http.Methods.GET, "/redir", priority = RoutePriority.LOWEST)
			fun redir(): String {
				throw Http.TemporalRedirect("/target")
			}
		}

		router.registerRoutes<TestRoute>()

		Assert.assertEquals(
			"307:Temporary Redirect:Headers((Content-Type, [text/html]), (Location, [/target])):",
			router.testRoute(Http.Method.GET, "/redir")
		)
	}

	@Test
	fun testParams() = syncTest {
		@Suppress("unused")
		class DemoRoute {
			@Route(Http.Methods.GET, "/test")
			fun test(@Get("name") name: String, @Get("demo") demo: Int, @Get("test") test: Int): String {
				return "hello $name$demo$test"
			}
		}

		router.registerRoutes<DemoRoute>()

		Assert.assertEquals(
				"200:OK:Headers((Content-Length, [13]), (Content-Type, [text/html])):hello world70",
				router.testRoute(Http.Method.GET, "/test?name=world&demo=7&test=a")
		)
	}
}