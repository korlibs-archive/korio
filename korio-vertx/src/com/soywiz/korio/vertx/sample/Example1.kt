package com.soywiz.korio.vertx.sample

import com.soywiz.korio.Korio
import com.soywiz.korio.ext.web.router.Header
import com.soywiz.korio.ext.web.router.KorRouter
import com.soywiz.korio.ext.web.router.Route
import com.soywiz.korio.ext.web.router.registerRouter
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.inject.Singleton
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.createHttpServer

object Example1 {
	@JvmStatic fun main(args: Array<String>) = Korio {
		val server = createHttpServer()
		val injector = AsyncInjector()
		val router = KorRouter(injector)
		println("Listening to 8090...")

		router.registerRouter<TestRoute>()

		server.listen(8090) { req ->
			router.handle(req)
		}
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
}
