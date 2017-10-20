package com.soywiz.korio

import kotlin.browser.window
import kotlin.js.Promise
import kotlin.test.Test

class KorioJsTest {
	@Test
	fun mykorioJsTest(): Promise<String> {
		return Promise { resolve, reject ->
			console.log("KorioJsTest.mykorioJsTest.start");
			global.setTimeout({
				console.log("KorioJsTest.mykorioJsTest.end");
				resolve("YAY!")
			}, 1000);
		}
	}
}