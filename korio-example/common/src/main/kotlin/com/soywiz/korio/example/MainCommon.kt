package com.soywiz.korio.example

import com.soywiz.korio.Korio
import com.soywiz.korio.ext.db.redis.Redis
import com.soywiz.korio.ext.db.redis.get
import com.soywiz.korio.ext.db.redis.set
import com.soywiz.korio.net.http.createHttpServer
import com.soywiz.korio.util.OS

object MainCommon {
	fun main() = Korio {
		println("Hello from MainCommon.main!")
		println("OS.name:${OS.nameLC}")
		println("OS.platformName:${OS.platformNameLC}")

		//val client = createHttpClient()
		////val indexHtmlContent = client.readString("index.html")
		//val indexHtmlContent = client.readString(
		//	if (OS.isBrowserJs) "index.html" else "https://www.google.es/"
		//)
		//println(indexHtmlContent.length)
		//println(indexHtmlContent)
		//println(indexHtmlContent.toList())

		println("[1]")
		val redis = Redis()
		println("[2]")
		redis.set("hello", "world")
		println("[3]")

		val server = createHttpServer()
			.httpHandler {
				//val rawBody = it.readRawBody()
				//it.end("yay! : " + rawBody.toString(UTF8))
				it.end("yay! : " + redis.get("world"))
			}
			.listen(8080)

		println("Listening at " + server.actualPort)
	}
}