package com.soywiz.korio.ext.web.cookie

import org.junit.Assert
import org.junit.Test

class KorCookiesTest {
	@Test
	fun name() {
		val setCookie = "id=a3fWa; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Secure; HttpOnly"

		Assert.assertEquals(
			KorCookies(LinkedHashMap(mapOf(
				"id" to "a3fWa",
				"Expires" to "Wed, 21 Oct 2015 07:28:00 GMT",
				"Secure" to null,
				"HttpOnly" to null
			))),
			KorCookies.parseSetCookies(listOf(setCookie))
		)

		Assert.assertEquals(
			setCookie,
			KorCookies.parseSetCookies(listOf(setCookie)).toString()
		)
	}
}