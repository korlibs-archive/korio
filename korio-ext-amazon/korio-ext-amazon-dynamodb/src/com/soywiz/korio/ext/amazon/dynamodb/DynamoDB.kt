package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.crypto.fromBase64
import com.soywiz.korio.crypto.toBase64
import com.soywiz.korio.error.ignoreErrors
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.expr.QExpr
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
// @TODO: Retry + Scale Capacity Units
class DynamoDB(val credentials: AmazonAuth.Credentials, val endpoint: URL?, val region: String, val client: HttpClient, val timeProvider: TimeProvider = TimeProvider()) {
	enum class Kind(val type: String) {
		STRING("S"), NUMBER("N"), BINARY("B"), BOOL("BOOL"),
		NULL("NULL"), MAP("M"), LIST("L"),
		STRING_SET("SS"), NUMBER_SET("NN"), BINARY_SET("BB");

		companion object {
			val BY_TYPE = values().map { it.type to it }.toMap()
		}
	}

	companion object {
		operator suspend fun invoke(region: String, endpoint: URL? = null, accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = createHttpClient(), timeProvider: TimeProvider = TimeProvider()): DynamoDB {
			return DynamoDB(AmazonAuth.getCredentials(accessKey, secretKey)!!, endpoint, region, httpClient, timeProvider)
		}
	}

	private fun serializeItem(obj: Any?): Any? = when (obj) {
		null -> mapOf(Kind.NULL.type to null)
		is String -> mapOf(Kind.STRING.type to obj)
		is ByteArray -> mapOf(Kind.BINARY.type to obj.toBase64())
		is Byte, is Short, is Int, is Double, is Long, is Float -> mapOf(Kind.NUMBER.type to obj)
		is Boolean -> mapOf(Kind.BOOL.type to obj)
		is Map<*, *> -> mapOf(Kind.MAP.type to obj.map { "${it.key}" to serializeItem(it.value) })
		is List<*> -> mapOf(Kind.LIST.type to obj.map { serializeItem(it) })
		else -> TODO("Unsupported type '$obj'")
	}

	private fun deserializeItem(obj: Any?): Any? = when (obj) {
		is Map<*, *> -> {
			val map = obj as Map<String, Any>
			val item = map.entries.first()
			val kind = Kind.BY_TYPE[item.key]
			when (kind) {
				Kind.STRING -> item.value as String
				Kind.BOOL -> item.value as Boolean
				Kind.BINARY -> (item.value as String).fromBase64()
				Kind.NUMBER -> Dynamic.toNumber(item.value)
				Kind.MAP -> {
					val base = item as Map<String, *>
					base.entries.map { it.key to deserializeItem(it.value) }
				}
				Kind.LIST -> {
					val base = item as List<*>
					base.map { deserializeItem(it) }
				}
				else -> TODO("Unimplemented deserializing type $kind")
			}
		}
		else -> invalidOp("Invalid operation. Not expected '$obj'")
	}

	private fun serializeObject(obj: Map<String, Any?>): Any? {
		return obj.map { it.key to serializeItem(it.value) }.toMap()
	}

	private fun deserializeObject(obj: Map<String, Any?>?): Map<String, Any?>? {
		return obj?.map { it.key to deserializeItem(it.value) }?.toMap()
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

	class Ctx {
		var lastId: Int = 0
		var args = hashMapOf<String, Any?>()
	}

	fun QExpr.build(ctx: Ctx): String {
		when (this) {
			is QExpr.EQ -> {
				val id = ctx.lastId++
				ctx.args[":v$id"] = value
				return "$key = :v$id"
			}
			is QExpr.AND -> {
				val ls = l.build(ctx)
				val rs = r.build(ctx)
				return "$ls AND $rs"
			}
			else -> TODO("TODO: $this")
		}
	}

	fun QExpr.buildWithCtx(): Pair<String, Map<String, Any?>> {
		val ctx = Ctx()
		val str = build(ctx)
		return str to ctx.args
	}

	suspend fun query(tableName: String, limit: Int? = null, expr: QExpr.Builder.() -> QExpr): List<Map<String, Any?>?> {
		//Expr { ("Id" eq 10) and ("Id" eq 10) }

		val exprRes = expr(QExpr.Builder()).buildWithCtx()

		val info = hashMapOf(
				"TableName" to tableName,
				"KeyConditionExpression" to exprRes.first,
				"ExpressionAttributeValues" to serializeObject(exprRes.second),
				"ReturnConsumedCapacity" to "TOTAL"
		)

		if (limit != null) info["Limit"] = limit

		val res = request("Query", info)

		return (res["Items"] as List<Any?>).map { deserializeObject(it as Map<String, Any?>?) }
	}

	suspend fun deleteItem(tableName: String, item: Map<String, Any?>): Map<String, Any?>? {
		val res = request("DeleteItem", mapOf(
				"TableName" to tableName,
				"Key" to serializeObject(item),
				"ReturnValues" to "ALL_OLD"
		))
		return deserializeObject(res["Attributes"] as Map<String, Any?>?)
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

	suspend fun createTable(name: String, types: Map<String, Kind>, hashKey: String, rangeKey: String? = null, readCapacityUnits: Int = 5, writeCapacityUnits: Int = 5) {
		val KeySchema = arrayListOf(
				mapOf("AttributeName" to hashKey, "KeyType" to "HASH")
		)

		if (rangeKey != null) {
			KeySchema += mapOf("AttributeName" to rangeKey, "KeyType" to "RANGE")
		}

		val res = request("CreateTable", mapOf(
				"TableName" to name,
				"AttributeDefinitions" to types.map { mapOf("AttributeName" to it.key, "AttributeType" to it.value.type) },
				"KeySchema" to KeySchema,
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

	suspend private fun request(target: String, payload: Map<String, Any?>): Map<String, Any?> {
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