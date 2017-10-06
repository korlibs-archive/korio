package com.soywiz.korio.vertx.sample

import com.soywiz.korio.ext.web.html.html
import com.soywiz.korio.ext.web.router.Header
import com.soywiz.korio.ext.web.router.Route
import com.soywiz.korio.ext.web.router.WsRoute
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer

@Suppress("unused")
class ExampleChatRoute {
	data class Client(val ws: HttpServer.WsRequest, val id: Long) {
		val name: String = "Client$id"
		fun send(msg: String) = ws.sendSafe(msg)
	}

	val clients = hashSetOf<Client>()
	private var lastClientId = 0L

	fun sendToAll(msg: String) {
		for (c in clients) c.send(msg)
	}

	@WsRoute("/chat")
	suspend fun chatWS(ws: HttpServer.WsRequest) {
		val me = Client(ws, lastClientId++)
		try {
			clients += me

			me.send("You are ${me.name}")
			sendToAll("${me.name} joined")

			for (msg in ws.stringMessageStream()) {
				sendToAll("${me.name} said: $msg")
			}
		} finally {
			clients -= me

			sendToAll("${me.name} left")
		}
	}

	@Route(Http.Methods.GET, "/chat")
	suspend fun chat(@Header("host") host: String): String {
		return html("""<html>
			<head>
				<title>
				</title>
			</head>
			<body>
			<form action='javascript:void(0)'>
				<input type='text' id='message' autofocus/>
				<input type='submit' id='send' value='Send' />
			</form>
			<div id='log' style='font:12px Monospace;'>
			</div>
			<script type="text/javascript">
				// Host: $host
				var ws = new WebSocket('ws://' + document.location.host + '/chat');
				var logElement = document.getElementById('log');
				function myconsoleLog(msg) {
					var div = document.createElement('div');
					div.innerText = msg;
					logElement.appendChild(div);
					console.log(msg);
				}
				ws.addEventListener('open', function(it) {
					myconsoleLog('ws.open');
				});
				ws.addEventListener('close', function(it) {
					myconsoleLog('ws.close');
				});
				ws.addEventListener('message', function(it) {
					myconsoleLog('ws.message: ' + it.data);
				});
				document.getElementById('send').addEventListener('click', function() {
					var messageInput = document.getElementById('message');
					ws.send(messageInput.value);
					messageInput.value = '';
				});

			</script>
			</body>
			</html>
		""")
	}
}
