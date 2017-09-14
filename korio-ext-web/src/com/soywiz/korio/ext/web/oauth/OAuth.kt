package com.soywiz.korio.ext.web.oauth

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.serialization.querystring.QueryString
import com.soywiz.korio.stream.openAsync

abstract class OAuth(val client: HttpClient, val redirectUri: String) {
	abstract val oauthBase: String
	abstract val oauthTokenBase: String
	abstract val oauthTokenInfoBase: String
	abstract val clientId: String
	abstract val clientSecret: String
	abstract val provider: String
	abstract val scope: String
	abstract val grantType: String
	abstract val tokenName: String

	abstract suspend fun getUserId(tokenId: String): String

	fun generateUrl(state: String): String {
		return this.oauthBase + '?' + QueryString.encode(
			"client_id" to this.clientId,
			"response_type" to "code",
			"scope" to this.scope,
			"redirect_uri" to redirectUri,
			"state" to state
		)
	}

	suspend fun getTokenId(code: String): String {
		val req = client.request(
			Http.Method.POST,
			oauthTokenBase,
			content = QueryString.encode(
				"code" to code,
				"client_id" to this.clientId,
				"client_secret" to this.clientSecret,
				"redirect_uri" to this.redirectUri,
				"grant_type" to this.grantType
			).openAsync(),
			headers = Http.Headers(
				"Content-Type" to "application/x-www-form-urlencoded"
			)
		)
		val parts = QueryString.decode(req.readAllString())
		return parts[tokenName]?.firstOrNull() ?: invalidOp("Can't find $tokenName in $parts")
	}
}