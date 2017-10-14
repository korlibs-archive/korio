package com.soywiz.korio.example

import com.soywiz.korio.Korio
import com.soywiz.korio.net.http.createHttpClient
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

		val server = createHttpServer()
			.httpHandler {
				it.end("yay!")
			}
			.listen(8080)

		println("Listening at " + server.actualPort)
	}
}