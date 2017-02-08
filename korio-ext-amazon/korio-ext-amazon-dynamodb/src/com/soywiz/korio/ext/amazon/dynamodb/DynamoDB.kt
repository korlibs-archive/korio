package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.error.invalidOp
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
// http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Operations.html
class DynamoDB(val credentials: AmazonAuth.Credentials, val endpoint: URL?, val region: String, val client: HttpClient, val timeProvider: TimeProvider = TimeProvider()) {
	enum class Kind(val type: String) {
		STRING("S"), NUMBER("N"), BINARY("B"), BOOL("BOOL"),
		NULL("NULL"), MAP("M"), LIST("L"),
		STRING_SET("SS"), NUMBER_SET("NN"), BINARY_SET("BB");
	}

	enum class KeyType { HASH, RANGE }

	companion object {
		operator suspend fun invoke(region: String, endpoint: URL? = null, accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = createHttpClient(), timeProvider: TimeProvider = TimeProvider()): DynamoDB {
			return DynamoDB(AmazonAuth.getCredentials(accessKey, secretKey)!!, endpoint, region, httpClient, timeProvider)
		}
	}

	private fun serializeItem(obj: Any?): Any? {
		return when (obj) {
			is Map<*, *> ->
				mapOf("M" to obj.map {
					"${it.key}" to serializeItem(it.value)
				})
			is String -> mapOf("S" to obj)
			else -> invalidOp("Invalid operation")
		}
	}

	private fun serializeObject(obj: Map<String, Any?>): Any? {
		return obj.map { it.key to serializeItem(it.value) }.toMap()
	}

	suspend fun putItems(tableName: String, vararg items: Map<String, Any?>) {
		when (items.size) {
			0 -> Unit
			1 -> putItem(tableName, items.first())
			else -> {
				val res = request("BatchWriteItem", mapOf(
						"RequestItems" to mapOf(
								tableName to items.map {
									mapOf(
											"PutRequest" to mapOf(
													"Item" to serializeObject(it)
											)
									)
								}
						),
						"ReturnConsumedCapacity" to "TOTAL"
				))
				println(res)
			}
		}
	}

	suspend fun putItem(tableName: String, item: Map<String, Any?>) {
		val res = request("PutItem ", mapOf(
				"TableName" to tableName,
				"Item" to serializeObject(item),
				"ReturnConsumedCapacity" to "TOTAL"
		))
		println(res)
	}

	suspend fun deleteTableIfExists(name: String) {
		ignoreErrors { deleteTable(name) }
	}

	suspend fun deleteTable(name: String) {
		val res = request("DeleteTable", mapOf(
				"TableName" to name
		))
		println(res)
	}

	suspend fun createTable(name: String, types: Map<String, Kind>, schema: Map<String, KeyType>, readCapacityUnits: Int = 5, writeCapacityUnits: Int = 5) {
		val res = request("CreateTable", mapOf(
				"TableName" to name,
				"AttributeDefinitions" to types.map { mapOf("AttributeName" to it.key, "AttributeType" to it.value.type) },
				"KeySchema" to schema.map { mapOf("AttributeName" to it.key, "KeyType" to it.value.name) },
				//"LocalSecondaryIndexes" to listOf<Any>(),
				"ProvisionedThroughput" to mapOf("ReadCapacityUnits" to readCapacityUnits, "WriteCapacityUnits" to writeCapacityUnits)
		))
		println(res)
	}

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
		println(payload)

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
			val info = try {
				Json.decode(resText) as Map<String, Any?>
			} catch (e: Throwable) {
				throw RuntimeException(resText)
			}
			val message = info.entries.firstOrNull { it.key.equals("message", ignoreCase = true) }?.value
			//println(info["__type"])
			throw RuntimeException("$message : " + resText)
		}
	}
}