package com.soywiz.korio.ext.web.oauth

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.serialization.querystring.QueryString
import com.soywiz.korio.stream.openAsync

abstract class OAuth(val client: HttpClient) {
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

	fun generateUrl(state: String, redirectUri: String): String {
		return this.oauthBase + '?' + QueryString.encode(
			"client_id" to this.clientId,
			"response_type" to "code",
			"scope" to this.scope,
			"redirect_uri" to redirectUri,
			"state" to state
		)
	}

	suspend fun getTokenId(code: String, redirectUri: String): String {
		val req = client.request(
			Http.Method.POST,
			oauthTokenBase,
			content = QueryString.encode(
				"code" to code,
				"client_id" to this.clientId,
				"client_secret" to this.clientSecret,
				"redirect_uri" to redirectUri,
				"grant_type" to this.grantType
			).openAsync(),
			headers = Http.Headers(
				"Content-Type" to "application/x-www-form-urlencoded"
			)
		)
		val str = req.readAllString()
		val parts = Json.decode(str) as Map<String, Any?>
		//val parts = QueryString.decode(str)
		return parts[tokenName]?.toString() ?: invalidOp("Can't find $tokenName in $str")
	}
}