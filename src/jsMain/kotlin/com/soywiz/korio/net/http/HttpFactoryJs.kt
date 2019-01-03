package com.soywiz.korio.net.http

import com.soywiz.korio.*
import com.soywiz.korio.util.*

internal actual val httpFactory: HttpFactory by lazy {
	object : HttpFactory {
		override fun createClient(): HttpClient = if (OS.isJsNodeJs) HttpClientNodeJs() else HttpClientBrowserJs()
		override fun createServer(): HttpServer = HttpSeverNodeJs()
	}
}
