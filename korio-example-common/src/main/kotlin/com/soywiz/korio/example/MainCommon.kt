package com.soywiz.korio.example

import com.soywiz.korio.Korio
import com.soywiz.korio.net.http.createHttpClient

object MainCommon {
	fun main() = Korio {
		println("Hello from MainCommon.main!")
		val client = createHttpClient()
		val indexHtmlContent = client.readString("index.html")
		println(indexHtmlContent.length)
		println(indexHtmlContent)
		println(indexHtmlContent.toList())
	}
}