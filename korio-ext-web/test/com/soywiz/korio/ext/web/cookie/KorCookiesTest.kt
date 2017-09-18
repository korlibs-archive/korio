package com.soywiz.korio.ext.web.cookie

import org.junit.Assert
import org.junit.Test

class KorCookiesTest {
	@Test
	fun name() {
		val setCookie = "id=a3fWa; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Secure; HttpOnly"

		Assert.assertEquals(
			KorCookie("id", "a3fWa"),
			KorCookie.parse(setCookie)
		)

		//Assert.assertEquals(setCookie, KorCookie.parse(setCookie).toString())
	}
}