package com.soywiz.korio.ext.s3

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.LogHttpClient
import com.soywiz.korio.util.TimeProvider
import org.junit.Assert
import org.junit.Test

class S3ClientTest {
	val httpClient = LogHttpClient()
	val timeProvider = TimeProvider { 1486426548734L }

	@Test
	fun name() = syncTest {
		val s3 = S3Vfs(region = "demo", accessKey = "myaccesskey", secretKey = "mysecretKey", httpClient = httpClient, timeProvider = timeProvider)
		Assert.assertEquals("LogHttpClient.response", s3["test/hello.txt"].readString())
		Assert.assertEquals(
				listOf("GET, https://s3-demo.amazonaws.com/test/hello.txt, Headers((date, Tue, 07 Feb 2017 01:15:48 CET), (authorization, AWS myaccesskey:rIjhHcIpcviAf1WwhmpE2L0j1YQ=)), null"),
				httpClient.log
		)
	}
}