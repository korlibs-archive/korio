package com.soywiz.korio.ext.web.oauth

import com.soywiz.korio.lang.Dynamic
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.serialization.querystring.QueryString

class FacebookOAuth(override val clientId: String, override val clientSecret: String, client: HttpClient = createHttpClient()) : OAuth(client) {
	override val oauthBase = "https://www.facebook.com/dialog/oauth"
	override val oauthTokenBase = "https://graph.facebook.com/oauth/access_token"
	override val oauthTokenInfoBase = ""
	override val provider = "facebook"
	override val scope = ""
	override val grantType = "authorization_code"
	override val tokenName = "access_token"

	suspend override fun getUserId(tokenId: String): String {
		val info = client.rest("https://graph.facebook.com").get("/me?" + QueryString.encode(tokenName to "tokenId"))
		return Dynamic.get(info, "id")?.toString() ?: ""
	}
}
