package com.soywiz.korio.ext.web.cookie

import org.junit.Test
import kotlin.test.assertEquals

class KorCookiesTest {
	@Test
	fun name() {
		val setCookie = "id=a3fWa; Expires=Wed, 21 Oct 2015 07:28:00 GMT; Secure; HttpOnly"

		assertEquals(
			KorCookie(name = "id", value = "a3fWa", expire = 1445412480000L, secure = true, httpOnly = true),
			KorCookie.parse(setCookie)
		)

		//assertEquals(setCookie, KorCookie.parse(setCookie).toString())
	}
}