@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.vfs.LocalVfs

object KorioFilewatchExample {
	@JvmStatic fun main(args: Array<String>) = EventLoop.main {
		val vfs = LocalVfs("""c:\temp""")
		println("Listening to... $vfs")
		vfs.jail().watch {
			println(it)
		}
	}
}