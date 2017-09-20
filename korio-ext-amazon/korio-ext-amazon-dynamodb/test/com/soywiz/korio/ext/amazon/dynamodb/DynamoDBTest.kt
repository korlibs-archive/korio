package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.lang.URL
import com.soywiz.korio.net.http.LogHttpClient
import com.soywiz.korio.util.TimeProvider
import org.junit.Test
import kotlin.test.assertEquals

class DynamoDBTest {
	val httpClient = LogHttpClient()
	val timeProvider = TimeProvider { 1486426548734L }

	@Test
	fun name() = syncTest {
		httpClient.setTextResponse("""{"TableNames":["comments","comments2"]}""")
		val db = DynamoDB("eu-west-1", accessKey = "testAccessKey", secretKey = "testSecretKey", timeProvider = timeProvider, httpClientFactory = { httpClient })
		assertEquals(listOf("comments", "comments2"), db.listTables())
		assertEquals(listOf(
			"""POST, https://dynamodb.eu-west-1.amazonaws.com, Headers((Authorization, [AWS4-HMAC-SHA256 Credential=testAccessKey/20170207/eu-west-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=280fcb0989bdebf9f70a61d8ed98f9045fccf150accdcfe099d1aabc8f9c899b]), (content-length, [2]), (Content-Type, [application/x-amz-json-1.0]), (host, [dynamodb.eu-west-1.amazonaws.com]), (x-amz-date, [20170207T001548Z]), (x-amz-target, [DynamoDB_20120810.ListTables])), {}"""
		), httpClient.getAndClearLog())
	}

	@Test
	fun customEndPoint() = syncTest {
		httpClient.setTextResponse("""{"TableNames":[]}""")
		val db = DynamoDB("eu-west-1", endpoint = URL("http://127.0.0.1:8000"), accessKey = "testAccessKey", secretKey = "testSecretKey", timeProvider = timeProvider, httpClientFactory = { httpClient })
		//val db = DynamoDB("eu-west-1", endpoint = URL("http://127.0.0.1:8000"))
		assertEquals(listOf<String>(), db.listTables())
		assertEquals(listOf(
			"""POST, http://127.0.0.1:8000, Headers((Authorization, [AWS4-HMAC-SHA256 Credential=testAccessKey/20170207/eu-west-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=f7fbbdcbaf70d741a70c783a34d534a8aba2f56499dcd79a7836ec14d1f2e823]), (content-length, [2]), (Content-Type, [application/x-amz-json-1.0]), (host, [127.0.0.1]), (x-amz-date, [20170207T001548Z]), (x-amz-target, [DynamoDB_20120810.ListTables])), {}"""
		), httpClient.getAndClearLog())
	}

	//@Test
	//fun createTableTest() = syncTest {
	//	val db = DynamoDB("eu-west-1", endpoint = URL("http://127.0.0.1:8000"))
	//	//val db = DynamoDB("eu-west-1")
	//	//db.deleteTableIfExists("Test2")
	//	//db.createTable("Test2", mapOf("test" to DynamoDB.Kind.STRING, "demo" to DynamoDB.Kind.STRING), hashKey = "test", rangeKey = "demo")
	//	//db.putItems("Test1", mapOf("test" to "hello"), mapOf("test1" to "hello"), mapOf("test2" to "hello"), mapOf("test3" to "hello"))
	//	/*
	//	db.putItem("Test1", mapOf("test" to "hello"))
	//	for (n in 0 until 100) {
	//		println(measureTimeMillis {
	//			db.putItem("Test1", mapOf("test" to "hello"))
	//		})
	//	}
	//	*/
	//	/*
	//	db.putItem("Test2", mapOf("test" to "hello", "demo" to "world"))
	//	db.putItem("Test2", mapOf("test" to "hello", "demo" to "world2"))
	//	db.putItem("Test2", mapOf("test" to "hello1", "demo" to "world"))
	//	db.putItem("Test2", mapOf("test" to "hello1", "demo" to "world2"))
	//	*/
//
	//	val items = db.query("Test2") { "test" eq "hello" }
	//	for (item in items) {
	//		println(item)
	//	}
	//	//println(db.deleteItem("Test1", mapOf("test" to "hello")))
	//}
}