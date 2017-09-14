package com.soywiz.korio.ext.web.oauth

import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.net.http.rest.rest
import com.soywiz.korio.serialization.querystring.QueryString
import com.soywiz.korio.util.asDynamicNode

class GoogleOAuth(override val clientId: String, override val clientSecret: String, redirectUri: String, client: HttpClient = createHttpClient()) : OAuth(client, redirectUri) {
	override val oauthBase = "https://accounts.google.com/o/oauth2/auth"
	override val oauthTokenBase = "https://accounts.google.com/o/oauth2/token"
	override val oauthTokenInfoBase = ""
	override val provider = "google"
	override val scope = "openid email"
	override val grantType = "authorization_code"
	override val tokenName = "id_token"

	override suspend fun getUserId(tokenId: String): String {
		val info = client.rest("https://www.googleapis.com").get("/oauth2/v1/tokeninfo?" + QueryString.encode(tokenName to "tokenId")).asDynamicNode()
		return info["user_id"].toString("")
	}
}