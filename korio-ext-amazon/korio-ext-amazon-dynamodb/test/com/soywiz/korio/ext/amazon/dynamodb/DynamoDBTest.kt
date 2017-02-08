package com.soywiz.korio.ext.amazon.dynamodb

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.LogHttpClient
import com.soywiz.korio.util.TimeProvider
import org.junit.Assert
import org.junit.Test

class DynamoDBTest {
	val httpClient = LogHttpClient()
	val timeProvider = TimeProvider { 1486426548734L }

	@Test
	fun name() = syncTest {
		httpClient.setTextResponse("""{"TableNames":["comments","comments2"]}""")
		val db = DynamoDB("eu-west-1", accessKey = "testAccessKey", secretKey = "testSecretKey", timeProvider = timeProvider, httpClient = httpClient)
		//val db = DynamoDB("eu-west-1")
		Assert.assertEquals(listOf("comments", "comments2"), db.listTables())
		Assert.assertEquals(listOf(
				"""POST, https://dynamodb.eu-west-1.amazonaws.com, Headers((Authorization, [AWS4-HMAC-SHA256 Credential=testAccessKey/20170207/eu-west-1/dynamodb/aws4_request, SignedHeaders=content-length;content-type;host;x-amz-date;x-amz-target, Signature=280fcb0989bdebf9f70a61d8ed98f9045fccf150accdcfe099d1aabc8f9c899b]), (content-length, [2]), (Content-Type, [application/x-amz-json-1.0]), (host, [dynamodb.eu-west-1.amazonaws.com]), (x-amz-date, [20170207T001548Z]), (x-amz-target, [DynamoDB_20120810.ListTables])), {}"""
		), httpClient.getAndClearLog())
	}
}