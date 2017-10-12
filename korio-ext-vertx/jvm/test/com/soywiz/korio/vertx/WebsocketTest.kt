package com.soywiz.korio.vertx

import com.soywiz.korio.async.sleep
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.async.waitOne
import com.soywiz.korio.net.ws.WebSocketClient
import io.vertx.core.http.HttpServer
import org.junit.Assert
import org.junit.Test
import java.net.URI

class WebsocketTest {
	@Test
	fun name() = syncTest {
		var out = ""

		val server = vx<HttpServer> {
			vertx.createHttpServer().websocketHandler { sws ->
				sws.writeFinalTextFrame("hello")
				sws.handler { msg ->
					out += "SERVER_RECV($msg)"
				}
			}.listen(0, it)
		}
		val port = server.actualPort()

		//println("Port: $port")

		val ws = WebSocketClient(URI("ws://127.0.0.1:$port"))

		out += "CLIENT_RECV(" + ws.onStringMessage.waitOne() + ")"

		ws.send("hey there!")

		sleep(100)

		vx<Void> { server.close(it) }
		ws.close()

		assertEquals("CLIENT_RECV(hello)SERVER_RECV(hey there!)", out)
	}
}