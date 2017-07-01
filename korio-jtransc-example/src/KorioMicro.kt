import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.localVfsProvider

fun main(args: Array<String>) = EventLoop {
	println("HELLO WORLD!")
	println(LocalVfs("c:/temp/wav1.wav").size())
	for (n in 0 until 5) {
		println("$n SECOND(S) LATER!")
		sleep(1000)
	}
}