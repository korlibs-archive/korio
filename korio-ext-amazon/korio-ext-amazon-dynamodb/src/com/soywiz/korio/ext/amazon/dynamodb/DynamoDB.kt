package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.ext.amazon.AmazonAuth
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.serialization.json.Json
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.Dynamic
import com.soywiz.korio.util.TimeProvider
import java.net.URL
import java.util.*

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.LowLevelAPI.html
class DynamoDB(val credentials: AmazonAuth.Credentials, val endpoint: URL?, val region: String, val client: HttpClient, val timeProvider: TimeProvider = TimeProvider()) {
	companion object {
		operator suspend fun invoke(region: String, endpoint: URL? = null, accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = createHttpClient(), timeProvider: TimeProvider = TimeProvider()): DynamoDB {
			return DynamoDB(AmazonAuth.getCredentials(accessKey, secretKey)!!, endpoint, region, httpClient, timeProvider)
		}
	}

	// http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Operations.html
	suspend fun listTables(): List<String> {
		//val res = request("ListTables", mapOf("ExclusiveStartTableName" to prefix, "Limit" to limit))
		val res = request("ListTables", mapOf())
		return Dynamic.toList(Dynamic.getAny(res, "TableNames")).map { "$it" }
	}

	suspend fun request(target: String, payload: Map<String, Any?>): Map<String, Any?> {
		//POST / HTTP/1.1
		//Host: dynamodb.<region>.<domain>;
		//Accept-Encoding: identity
		//Content-Length: <PayloadSizeBytes>
		//User-Agent: <UserAgentString>
		//Content-Type: application/x-amz-json-1.0
		//Authorization: AWS4-HMAC-SHA256 Credential=<Credential>, SignedHeaders=<Headers>, Signature=<Signature>
		//X-Amz-Date: <Date>
		//X-Amz-Target: DynamoDB_20120810.ListTables
		//
		//{
		//	"ExclusiveStartTableName": "Forum",
		//	"Limit": 3
		//}

		val content = Json.encode(payload).toByteArray()


		val url = endpoint ?: URL("https://dynamodb.$region.amazonaws.com")

		val res = client.request(Http.Method.POST, url.toString(),
				headers = AmazonAuth.V4.signHeaders(
						credentials.accessKey,
						credentials.secretKey,
						Http.Method.POST,
						url,
						Http.Headers(
								"Content-Type" to "application/x-amz-json-1.0",
								"x-amz-date" to AmazonAuth.V4.DATE_FORMAT.format(Date(timeProvider.currentTimeMillis())),
								"x-amz-target" to "DynamoDB_20120810.$target",
								"host" to url.host,
								"content-length" to "${content.size}"
						),
						content, region, "dynamodb"
				),
				content = content.openAsync()
		)

		val resText = res.readAllBytes().toString(Charsets.UTF_8)

		if (res.success) {
			return Json.decode(resText) as Map<String, Any?>
		} else {
			throw RuntimeException(resText)
		}
	}
}