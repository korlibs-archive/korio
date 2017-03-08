import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.inject.AsyncInjector
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.vertx.router.*
import com.soywiz.korio.vertx.vertx
import com.soywiz.korio.vfs.ResourcesVfs
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.handler.StaticHandler

fun main(args: Array<String>) = EventLoop {
	val injector = AsyncInjector()
			.map(vertx)
			.map(vertx.fileSystem())

	routedHttpServer(injector) {
		println("Registering routes...")
		//val eventLog = injector.get<EventLog>()
		registerInterceptor { req, map ->
			//eventLog.log(req.path(), map)
		}
		registerRouter<TestRoute>()
		router.route().handler(StaticHandler.create("public").setCachingEnabled(true))
		println("Ok")

		// Static handler!
		//route("/assets/*").handler(StaticHandler.create("assets"));
	}
}

class TestRoute {
	@Route(HttpMethod.GET, "/")
	fun test(auth: Http.Auth, response: Http.Response, @Header("authorization") authorization: String): String {
		//fun test(@Header("accept") accept: String, @Header("authorization") authorization: String, headers: Http.Headers) {
		//fun test() {
		//header('WWW-Authenticate: Basic realm="Mi dominio"');
		//header('HTTP/1.0 401 Unauthorized');
		auth.checkBasic { user == "test" && pass == "test" }
		//if (!auth.validate(expectedUser = "test", expectedPass = "test", realm = "")) Http.HttpException.unauthorizedBasic(realm = "Domain", msg = "Invalid auth")


		response.header("X-Yay", "yay!")
		response.header("X-Authorization-Echo", authorization)

		return "yay!"
	}
}
