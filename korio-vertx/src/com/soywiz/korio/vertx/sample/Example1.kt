package com.soywiz.korio.vertx.sample

import com.soywiz.korio.Korio
import com.soywiz.korio.ext.web.cookie.supportCookies
import com.soywiz.korio.ext.web.html.html
import com.soywiz.korio.ext.web.router.*
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpServer
import com.soywiz.korio.net.http.createHttpServer

object Example1 {
	@JvmStatic
	fun main(args: Array<String>) = Korio {
		val injector = AsyncInjector()
		val server = createHttpServer()
			.router(injector) {
				registerRoutes<TestRoute>()
				registerRoutes<ExampleChatRoute>()
			}
			.listen(System.getenv("PORT")?.toIntOrNull() ?: 8080)
		println("Listening to ${server.actualPort}...")
	}
}

@Singleton
class AuthRepository {
	suspend fun check(user: String, pass: String): Boolean {
		return user == pass
	}
}

@Suppress("unused")
class TestRoute(
	val authRepository: AuthRepository
) {
	@Route(Http.Methods.GET, "/")
	suspend fun test(auth: Http.Auth, response: Http.Response, @Header("authorization") authorization: String): String {
		//fun test(@Header("accept") accept: String, @Header("authorization") authorization: String, headers: Http.Headers) {
		//fun test() {
		//header('WWW-Authenticate: Basic realm="Mi dominio"');
		//header('HTTP/1.0 401 Unauthorized');
		auth.checkBasic { authRepository.check(user, pass) }
		//if (!auth.validate(expectedUser = "test", expectedPass = "test", realm = "")) Http.HttpException.unauthorizedBasic(realm = "Domain", msg = "Invalid auth")


		response.header("X-Yay", "yay!")
		response.header("X-Authorization-Echo", authorization)

		return "yay!"
	}

	@Route(Http.Methods.GET, "/hello/:name")
	suspend fun helloName(@Param("name") name: String): String {
		return "Hello $name"
	}

	@WsRoute("/echo")
	suspend fun echoWS(ws: HttpServer.WsRequest) {
		ws.onStringMessage {
			ws.send(it)
		}
	}

	@Route(Http.Methods.GET, "/echo")
	suspend fun echo(): String {
		return html("""<html>
<head>
	<title>
	</title>
</head>
<body>
<p>
	<strong>Open developer console to see events</strong>
</p>
<form action='javascript:void(0)'>
	<input type='text' id='message' autofocus/>
	<input type='submit' id='send' value='Send' />
</form>
<script type="text/javascript">
	var ws = new WebSocket('ws://127.0.0.1:8090/echo');
	ws.addEventListener('open', function(it) {
		console.log('ws.open');
	});
	ws.addEventListener('close', function(it) {
		console.log('ws.close');
	});
	ws.addEventListener('message', function(it) {
		console.log('ws.message', it.data);
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
