@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.map
import com.soywiz.korio.async.spawn
import com.soywiz.korio.async.toList
import com.soywiz.korio.stream.readU8
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korio.vfs.UrlVfs
import com.soywiz.korio.vfs.openAsIso

//class Test {
//	companion object {
//		fun main(callback: Test.() -> Unit) {
//		}
//	}
//}
//
//fun demo() = Test.main {
//	val a = this@main
//}

fun main(args: Array<String>) = EventLoop {
	val eventLoop = this@EventLoop
	//println(LocalVfs("/").list().toList())

	val url = UrlVfs("http://127.0.0.1:8080")

	println(url["hello.txt"].stat())

	println(ResourcesVfs.list().map { it.fullname }.toList())
	//println(ResourcesVfs["hello.txt"].readString())

	val file = url["hello.txt"]

	val process = spawn(eventLoop.coroutineContext) {
		file.openUse {
			while (!eof()) {
				val c = readU8()
				println("" + c.toChar() + " : " + c)
			}
		}
	}

	println(url["hello.txt"].readString())

	println("Yeah! I'm going to open an iso file from an url without downloading all the iso lazily and asynchronously working on Browser/Node.JS and JVM.")
	println("But the code looks like synchronous!")
	val iso = url["isotest.iso"].openAsIso()

	for (item in iso.listRecursive()) {
		println("$item : ${item.isDirectory()} : ${item.size()}")
	}

	println("" + iso["hello/world.txt"] + " : " + iso["hello/world.txt"].readString())

	process.await()

	println(ResourcesVfs)
	println("From resources:" + ResourcesVfs["hello.txt"].readString())

	//val server = AsyncServer(7778)
	//
	//println("Listening at ${server.port}")
	//
	//executeInWorker {
	//	println("HELLO!")
	//}
	//
	//spawn {
	//	for (client in server.listen()) {
	//		println("client connected!")
	//		spawn {
	//			println("client connected 2!")
	//			client.writeString("HELLO WORLD!")
	//		}
	//	}
	//}

	//val resources = ResourcesVfs
	//val url = UrlVfs("http://google.es/")
	////val url = UrlVfs("http://127.0.0.1:8080/")
	//val res = url.readString()
	//println(res.length)
	//println(res)
	//for (n in 0 until res.length) println("" + res[n].toInt() + " : " + res[n])

	//resources["program.js"].read()
	//println("Hello world!")
}
