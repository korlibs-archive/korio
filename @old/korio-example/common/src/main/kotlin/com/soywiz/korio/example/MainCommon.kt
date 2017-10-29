package com.soywiz.korio.example

import com.soywiz.korio.Korio
import com.soywiz.korio.ext.web.router.*
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.createHttpServer
import com.soywiz.korio.util.OS

object MainCommon {
	fun main() = Korio {
		println("Hello from MainCommon.main!")
		println("OS.name:${OS.rawNameLC}")
		println("OS.platformName:${OS.platformNameLC}")

		//val client = createHttpClient()
		////val indexHtmlContent = client.readString("index.html")
		//val indexHtmlContent = client.readString(
		//	if (OS.isBrowserJs) "index.html" else "https://www.google.es/"
		//)
		//println(indexHtmlContent.length)
		//println(indexHtmlContent)
		//println(indexHtmlContent.toList())

		//println("[1]")
		//val redis = Redis()
		//println("[2]")
		//redis.set("hello", "world")
		//println("[3]")

		val injector = AsyncInjector()

		val server = createHttpServer()
			//.httpHandler {
			//	val rawBody = it.readRawBody()
			//	//it.end("yay! : " + rawBody.toString(UTF8))
			//	//it.end("yay! : " + redis.get("hello"))
			//	it.addHeader("Content-Type", "text/html")
			//	//it.end("yay! : " + redis.get("hello") + "<form action='/' method='post'><input type='text' name='name' /><input type='submit' /></form> BODY: ${rawBody.toString(UTF8)}")
			//	it.end("yay! : " + "<form action='/' method='post'><input type='text' name='name' /><input type='submit' /></form> BODY: ${rawBody.toString(UTF8)}")
			//	//it.end("yay!")
			//}
			.router(injector) {
				get("/") { res ->
					res.end("root : " + res.getHeader("accept"))
				}
				get("/test") { res ->
					res.end("test")
				}
				get("/user/:name") { res ->
					val name = res.pathParam("name")
					res.end("user: $name")
				}
			}
			.listen(8080)

		println("Listening at " + server.actualPort)
	}
}