package com.soywiz.korio.ext.web.oauth

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.FakeHttpClient
import org.junit.Test
import kotlin.test.assertEquals

class GoogleOAuthTest {
	val client = FakeHttpClient()
	val oauth = GoogleOAuth(clientId = "myclient", clientSecret = "mysecret", client = client)
	val redirUrl = "https://myredir"

	@Test
	fun testGetTokenUserId() = syncTest {
		assertEquals(
			listOf("""GET:https://www.googleapis.com/oauth2/v1/tokeninfo?id_token=tokenId:null"""),
			client.capture {
				client.addOkResponse("{\"user_id\":\"myuserid\"}")
				assertEquals("myuserid", oauth.getUserId("mytoken"))
			}
		)
	}

	@Test
	fun testGetTokenId() = syncTest {
		assertEquals(
			listOf("""POST:https://accounts.google.com/o/oauth2/token:code=mycode&client_id=myclient&client_secret=mysecret&redirect_uri=https%3A%2F%2Fmyredir&grant_type=authorization_code"""),
			client.capture {
				client.addOkResponse("{\"id_token\":\"mytoken\"}")
				assertEquals("mytoken", oauth.getTokenId("mycode", redirUrl))
			}
		)
	}

	@Test
	fun testGenerateUrl() {
		assertEquals(
			"https://accounts.google.com/o/oauth2/auth?client_id=myclient&response_type=code&scope=openid+email&redirect_uri=https%3A%2F%2Fmyredir&state=state",
			oauth.generateUrl("state", redirUrl)
		)
	}
}