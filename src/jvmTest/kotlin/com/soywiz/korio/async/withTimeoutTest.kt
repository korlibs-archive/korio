package com.soywiz.korio.async

import kotlinx.coroutines.*
import java.util.concurrent.*
import java.util.concurrent.CancellationException
import kotlin.test.*

class withTimeoutTest {
	@Test
	@Ignore("Flaky")
	fun noTimeout() = suspendTest {
		var out = ""
		var result = "none"
		try {
			result = withTimeout(300, TimeUnit.MILLISECONDS) {
				out += "a"
				delay(20)
				out += "b"
				delay(50)
				out += "c"
				delay(100)
				out += "d"
				"done"
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}

		delay(650)
		assertEquals("abcd", out)
		assertEquals("done", result)
	}

	@Test
	@Ignore("Flaky")
	fun simple() = suspendTest {
		var out = ""
		try {
			withTimeout(100, TimeUnit.MILLISECONDS) {
				out += "a"
				delay(20)
				out += "b"
				delay(50)
				out += "c"
				delay(100)
				out += "d"
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}

		delay(300)
		assertEquals("abc<CANCEL>", out)
	}

	@Test
	@Ignore("Flaky")
	fun name() = suspendTest {
		var out = ""
		try {
			out += "0"
			withTimeout(50, TimeUnit.MILLISECONDS) {
				try {
					withTimeout(100, TimeUnit.MILLISECONDS) {
						out += "a"
						delay(20)
						out += "b"
						delay(50)
						out += "c"
						delay(100)
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
		delay(300)
		assertEquals("0ab<CANCEL1><CANCEL2>", out)
	}

	@Test
	@Ignore("Flaky")
	fun name2() = suspendTest {
		var out = ""
		try {
			withTimeout(200) {
				//out += UrlVfs("http://127.0.0.2:1337/test").readString() // fails on travis
				delay(10000)
				out += "test"
			}
		} catch (e: CancellationException) {
			out += "<CANCEL>"
		}
		assertEquals("<CANCEL>", out)
	}
}