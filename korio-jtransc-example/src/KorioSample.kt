import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.async
import com.soywiz.korio.async.executeInWorker
import com.soywiz.korio.async.spawn
import com.soywiz.korio.net.AsyncServer
import com.soywiz.korio.stream.writeString

fun main(args: Array<String>) = EventLoop.main {
	val server = AsyncServer(7778)

	println("Listening at ${server.port}")

	executeInWorker {
		println("HELLO!")
	}

	spawn {
		for (client in server.listen()) {
			println("client connected!")
			spawn {
				println("client connected 2!")
				client.writeString("HELLO WORLD!")
			}
		}
	}

	//val resources = ResourcesVfs()
	//val url = UrlVfs("http://google.es/")
	////val url = UrlVfs("http://127.0.0.1:8080/")
	//val res = url.readString()
	//println(res.length)
	//println(res)
	//for (n in 0 until res.length) println("" + res[n].toInt() + " : " + res[n])

	//resources["program.js"].read()
	//println("Hello world!")
}