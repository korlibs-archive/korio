package com.soywiz.korio.ext.web.router

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.ext.web.cookie.registerCookies
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.jvmAutomapping
import com.soywiz.korio.net.http.FakeRequest
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.vfs.MemoryVfsMix
import com.soywiz.korio.vfs.VfsFile
import org.junit.Test
import kotlin.test.assertEquals

class KorRouterTest {
	val injector = AsyncInjector().jvmAutomapping()
	val requestConfig = HttpServer.RequestConfig()
	val router = KorRouter(injector, requestConfig)

	suspend private fun KorRouter.testRoute(method: Http.Method, uri: String, headers: Http.Headers = Http.Headers(), body: ByteArray = ByteArray(0)): String {
		val request = FakeRequest(method, uri, headers, body, requestConfig)
		//println("Accepting:")
		router.accept(request)
		//println("Accepted:")
		//println(request.log.joinToString("\n"))
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
			fun test2() = lmapOf("hello" to "world")
		}

		router.registerRoutes<DemoRoute>()

		assertEquals(
			"200:OK:Headers((Content-Length, [11]), (Content-Type, [text/html])):hello world",
			router.testRoute(Http.Method.GET, "/hello/world")
		)

		assertEquals(
			"200:OK:Headers((Content-Length, [15]), (Content-Type, [text/plain])):hellotext world",
			router.testRoute(Http.Method.GET, "/hellotext/world")
		)

		assertEquals(
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

			@Route(Http.Methods.ALL, "*", priority = RoutePriority.LOWEST)
			fun static(req: HttpServer.Request): VfsFile {
				val file = files[req.path]
				return file
			}
		}

		router.registerRoutes<StaticRoute>()

		assertEquals(
			"200:OK:Headers((Accept-Ranges, [bytes]), (Content-Length, [13]), (Content-Type, [text/plain]), (ETag, [b7a9adb9fec4116c29da690c45d9a67df535af9d-0-13]), (Last-Modified, [Thu, 01 Jan 1970 00:00:00 UTC])):User-agent: *",
			router.testRoute(Http.Method.GET, "/robots.txt")
		)

		assertEquals(
			"200:OK:Headers((Accept-Ranges, [bytes]), (Content-Length, [13]), (Content-Type, [text/plain]), (ETag, [b7a9adb9fec4116c29da690c45d9a67df535af9d-0-13]), (Last-Modified, [Thu, 01 Jan 1970 00:00:00 UTC])):User-agent: *",
			router.testRoute(Http.Method.GET, "/robots.txt?v=3.2.0")
		)

		assertEquals(
			"404:Not Found:Headers((Content-Length, [30]), (Content-Type, [text/html])):404 - Not Found - /donotexists",
			router.testRoute(Http.Method.GET, "/donotexists")
		)

		// Partial content: GET
		assertEquals(
			"206:Partial Content:Headers((Accept-Ranges, [bytes]), (Content-Length, [2]), (Content-Range, [bytes 1-2/13]), (Content-Type, [text/plain]), (ETag, [b7a9adb9fec4116c29da690c45d9a67df535af9d-0-13]), (Last-Modified, [Thu, 01 Jan 1970 00:00:00 UTC])):se",
			router.testRoute(Http.Method.GET, "/robots.txt", Http.Headers("Range" to "bytes=1-2"))
		)

		// Partial content: HEAD
		assertEquals(
			"206:Partial Content:Headers((Accept-Ranges, [bytes]), (Content-Length, [0]), (Content-Range, [bytes 1-2/13]), (Content-Type, [text/plain]), (ETag, [b7a9adb9fec4116c29da690c45d9a67df535af9d-0-13]), (Last-Modified, [Thu, 01 Jan 1970 00:00:00 UTC])):",
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

		assertEquals(
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

		assertEquals(
			"200:OK:Headers((Content-Length, [13]), (Content-Type, [text/html])):hello world70",
			router.testRoute(Http.Method.GET, "/test?name=world&demo=7&test=a")
		)
	}

	@Test
	fun testCookies() = syncTest {
		@Suppress("unused")
		class DemoRoute {
			@Route(Http.Methods.GET, "/")
			fun test(): String = "demo"
		}

		router.registerCookies()
		router.registerRoutes<DemoRoute>()

		assertEquals(
			"200:OK:Headers((Content-Length, [4]), (Content-Type, [text/html]), (Set-Cookie, [hello=world])):demo",
			router.testRoute(Http.Method.GET, "/", Http.Headers("Cookie" to "hello=world"))
		)
	}
}