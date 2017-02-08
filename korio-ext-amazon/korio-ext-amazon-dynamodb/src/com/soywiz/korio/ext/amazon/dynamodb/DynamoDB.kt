package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.ext.amazon.AmazonAuth
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.createHttpClient

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.LowLevelAPI.html
class DynamoDB(val credentials: AmazonAuth.Credentials?, val region: String) {
	val client = createHttpClient()

	companion object {
		suspend fun invoke(region: String, accessKey: String? = null, secretKey: String? = null): DynamoDB {
			return DynamoDB(AmazonAuth.getCredentials(accessKey, secretKey), region)
		}
	}

	//suspend fun request() {
	//	client.request(Http.Method, )
	//}
}