import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.UrlVfs

fun main(args: Array<String>) = EventLoop.main {
	//val resources = ResourcesVfs()
	val url = UrlVfs("http://google.es/")
	//val url = UrlVfs("http://127.0.0.1:8080/")
	val res = url.readString()
	println(res.length)
	println(res)
	//for (n in 0 until res.length) println("" + res[n].toInt() + " : " + res[n])

	//resources["program.js"].read()
	//println("Hello world!")
}