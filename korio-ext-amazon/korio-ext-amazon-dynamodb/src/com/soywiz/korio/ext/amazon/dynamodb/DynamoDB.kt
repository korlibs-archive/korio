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
import com.soywiz.korio.util.ClassFactory
import com.soywiz.korio.util.Dynamic
import com.soywiz.korio.util.TimeProvider
import java.net.URL
import java.util.*

// http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.LowLevelAPI.html
// http://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Operations.html
// @TODO: Retry + Scale Capacity Units
class DynamoDB(val credentials: AmazonAuth.Credentials, val endpoint: URL?, val region: String, val client: HttpClient, val timeProvider: TimeProvider = TimeProvider()) {
	// @TODO: Add createTable for Class<T>
	@Target(AnnotationTarget.FIELD)
	annotation class HashKey

	@Target(AnnotationTarget.FIELD)
	annotation class RangeKey

	enum class Kind(val type: String) {
		STRING("S"), NUMBER("N"), BINARY("B"), BOOL("BOOL"),
		NULL("NULL"), MAP("M"), LIST("L"),
		STRING_SET("SS"), NUMBER_SET("NN"), BINARY_SET("BB");

		companion object {
			val BY_TYPE = values().map { it.type to it }.toMap()

			fun fromType(clazz: Class<*>?): Kind {
				return when (clazz) {
					null -> Kind.NULL
					java.lang.String::class.java -> Kind.STRING
					java.lang.Boolean.TYPE, java.lang.Boolean::class.java -> Kind.BOOL
					java.lang.Double.TYPE, java.lang.Double::class.java -> Kind.NUMBER
					java.lang.Float.TYPE, java.lang.Float::class.java -> Kind.NUMBER
					java.lang.Integer.TYPE, java.lang.Integer::class.java -> Kind.NUMBER
					java.lang.Long.TYPE, java.lang.Long::class.java -> Kind.NUMBER
					ByteArray::class.java -> Kind.BINARY
					else -> when {
						clazz.isAssignableFrom(Map::class.java) -> Kind.MAP
						clazz.isAssignableFrom(List::class.java) -> Kind.LIST
						else -> invalidOp("Unsupported type $clazz")
					}
				}
			}
		}
	}


	companion object {
		operator suspend fun invoke(region: String, endpoint: URL? = null, accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = createHttpClient(), timeProvider: TimeProvider = TimeProvider()): DynamoDB {
			return DynamoDB(AmazonAuth.getCredentials(accessKey, secretKey)!!, endpoint, region, httpClient, timeProvider)
		}
	}

	class Typed<T>(val db: DynamoDB, val clazz: Class<T>, val tableName: String) {
		val classFactory = ClassFactory(clazz)

		suspend fun createIfNotExists(readCapacityUnits: Int = 5, writeCapacityUnits: Int = 5) {
			val hashKeyField = classFactory.fields.firstOrNull { it.getAnnotation(HashKey::class.java) != null } ?: invalidOp("No fields from $clazz contain @HashKey")
			val rangeKeyField = classFactory.fields.firstOrNull { it.getAnnotation(RangeKey::class.java) != null }

			val hashKey = hashKeyField.name to Kind.fromType(hashKeyField.type)
			val rangeKey = if (rangeKeyField != null) rangeKeyField.name to Kind.fromType(rangeKeyField.type) else null

			db.createTableIfNotExists(
					tableName,
					hashKey = hashKey,
					rangeKey = rangeKey,
					readCapacityUnits = readCapacityUnits,
					writeCapacityUnits = writeCapacityUnits
			)
		}

		suspend fun query(limit: Int? = null, expr: QExpr.TypedBuilder<T>.() -> QExpr): List<T> {
			return db.query(tableName, limit = limit, expr = {
				expr(QExpr.TypedBuilder())
			}).filterNotNull().map { classFactory.create(it) }
		}

		suspend fun put(vararg items: T) {
			db.putItems(tableName, *items.map { classFactory.toMap(it) }.toTypedArray())
		}
	}

	inline fun <reified T> typed(tableName: String) = Typed(this, T::class.java, tableName)
	fun <T> typed(clazz: Class<T>, tableName: String) = Typed(this, clazz, tableName)

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
						)
						//,"ReturnConsumedCapacity" to "TOTAL"
				))
				//println(res)
			}
		}
	}

	class Ctx {
		var lastId: Int = 0
		var args = hashMapOf<String, Any?>()
	}

	fun QExpr.build(ctx: Ctx): String {
		when (this) {
			is QExpr.BINOP -> {
				val id = ctx.lastId++
				ctx.args[":v$id"] = value
				return "$key ${this.op} :v$id"
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
		request("PutItem ", mapOf(
				"TableName" to tableName,
				"Item" to serializeObject(item)
				//,"ReturnConsumedCapacity" to "TOTAL"
		))
		//println(res)
	}

	suspend fun deleteTableIfExists(name: String) {
		ignoreErrors { deleteTable(name) }
	}

	suspend fun deleteTable(name: String) {
		request("DeleteTable", mapOf(
				"TableName" to name
		))
		//println(res)
	}

	suspend fun createTableIfNotExists(name: String, hashKey: Pair<String, Kind>, rangeKey: Pair<String, Kind>? = null, readCapacityUnits: Int = 5, writeCapacityUnits: Int = 5) {
		try {
			createTable(name, hashKey, rangeKey, readCapacityUnits, writeCapacityUnits)
		} catch (e: ResourceInUseException) {
		}
	}

	suspend fun createTable(name: String, hashKey: Pair<String, Kind>, rangeKey: Pair<String, Kind>? = null, readCapacityUnits: Int = 5, writeCapacityUnits: Int = 5) {
		val AttributeDefinitions = arrayListOf(mapOf("AttributeName" to hashKey.first, "AttributeType" to hashKey.second.type))
		val KeySchema = arrayListOf(mapOf("AttributeName" to hashKey.first, "KeyType" to "HASH"))

		if (rangeKey != null) {
			AttributeDefinitions += mapOf("AttributeName" to rangeKey.first, "AttributeType" to rangeKey.second.type)
			KeySchema += mapOf("AttributeName" to rangeKey.first, "KeyType" to "RANGE")
		}

		request("CreateTable", mapOf(
				"TableName" to name,
				"AttributeDefinitions" to AttributeDefinitions,
				"KeySchema" to KeySchema,
				//"LocalSecondaryIndexes" to listOf<Any>(),
				"ProvisionedThroughput" to mapOf("ReadCapacityUnits" to readCapacityUnits, "WriteCapacityUnits" to writeCapacityUnits)
		))
		//println(res)
	}

	suspend fun listTables(): List<String> {
		//val res = request("ListTables", mapOf("ExclusiveStartTableName" to prefix, "Limit" to limit))
		val res = request("ListTables", mapOf())
		return Dynamic.toList(Dynamic.getAny(res, "TableNames")).map { "$it" }
	}

	suspend private fun request(target: String, payload: Map<String, Any?>): Map<String, Any?> {
		val contentJson = Json.encode(payload)
		val content = contentJson.toByteArray()
		//println(contentJson)

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
			val type = info["__type"] ?: "RuntimeException"
			val message = info.entries.firstOrNull { it.key.equals("message", ignoreCase = true) }?.value
			val msg = "$message : " + resText
			//println()

			when (type) {
				"com.amazonaws.dynamodb.v20120810#ResourceInUseException" -> throw ResourceInUseException(msg)
				"com.amazonaws.dynamodb.v20120810#ProvisionedThroughputExceededException" -> throw ProvisionedThroughputExceededException(msg)
				else -> throw RuntimeException(msg)
			}
		}
	}

	open class AmazonException(msg: String) : RuntimeException(msg)
	class ProvisionedThroughputExceededException(msg: String) : AmazonException(msg)
	class ResourceInUseException(msg: String) : AmazonException(msg)
}