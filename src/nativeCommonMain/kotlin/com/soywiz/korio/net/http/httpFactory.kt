package com.soywiz.korio.net.http

import com.soywiz.korio.*

internal actual val httpFactory: HttpFactory = object : HttpFactory {
	override fun createClient(): HttpClient = NativeHttpClient()
	override fun createServer(): HttpServer = HttpServerPortable.create()
}

