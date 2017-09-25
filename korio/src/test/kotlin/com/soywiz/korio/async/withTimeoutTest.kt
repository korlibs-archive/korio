package com.soywiz.korio.async

import org.junit.Test
import java.util.concurrent.CancellationException
import kotlin.test.assertEquals

class withTimeoutTest {
	@Test
	fun simple() = syncTest {
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
		assertEquals("abc<CANCEL>", out)
	}

	@Test
	fun name() = syncTest {
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
		assertEquals("0ab<CANCEL1><CANCEL2>", out)
	}

	@Test
	fun name2() = syncTest {
		var out = ""
		try {
			withTimeout(200) {
				//out += UrlVfs("http://127.0.0.2:1337/test").readString() // fails on travis
				sleep(10000)
				out += "test"
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}
		assertEquals("<CANCEL>", out)
	}
}