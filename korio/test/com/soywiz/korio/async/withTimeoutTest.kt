package com.soywiz.korio.async

import com.soywiz.korio.vfs.UrlVfs
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CancellationException

class withTimeoutTest {
	@Test
	fun simple() = sync {
		var out = ""
		try {
			withTimeout(100, name = "timeout2") {
				out += "a"
				sleep(20)
				out += "b"
				sleep(50)
				out += "c"
				sleep(100)
				out += "d"
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}

		sleep(300)
		Assert.assertEquals("abc<CANCEL>", out)
	}

	@Test
	fun name() = sync {
		var out = ""
		try {
			out += "0"
			withTimeout(50, name = "timeout1") {
				try {
					withTimeout(100, name = "timeout2") {
						out += "a"
						sleep(20)
						out += "b"
						sleep(50)
						out += "c"
						sleep(100)
						out += "d"
					}
				} catch (e: CancellationException) {
					out += "<CANCEL1>"
				}
			}
			out += "1"
		} catch (e: CancellationException) {
			out += "<CANCEL2>"
		}
		sleep(300)
		Assert.assertEquals("0ab<CANCEL1><CANCEL2>", out)
	}

	@Test
	fun name2() = sync {
		var out = ""
		try {
			withTimeout(200) {
				out += UrlVfs("http://127.0.0.2:1337/test").readString()
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}
		Assert.assertEquals("<CANCEL>", out)
	}
}