package com.soywiz.korio.net

import com.soywiz.korio.*

actual val asyncSocketFactory: AsyncSocketFactory by lazy {
	object : AsyncSocketFactory() {
		override suspend fun createClient(secure: Boolean): AsyncClient = NodeJsAsyncClient()
		override suspend fun createServer(port: Int, host: String, backlog: Int, secure: Boolean): AsyncServer =
			NodeJsAsyncServer().init(port, host, backlog)
	}
}