package com.soywiz.korio.ext.s3

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.LogHttpClient
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.MimeType
import com.soywiz.korio.vfs.VfsFile
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class S3Test {
	val httpClient = LogHttpClient()
	val timeProvider = TimeProvider { 1486426548734L }
	lateinit var s3: VfsFile

	@Before
	fun setUp() = syncTest {
		s3 = S3Vfs(region = "demo", accessKey = "myaccesskey", secretKey = "mysecretKey", httpClient = httpClient, timeProvider = timeProvider)
	}

	@Test
	fun checkGet() = syncTest {
		httpClient.response = httpClient.response.withStringResponse("hello")

		Assert.assertEquals("hello", s3["test/hello.txt"].readString())
		Assert.assertEquals(
				listOf("GET, https://test.s3.amazonaws.com/hello.txt, Headers((Authorization, [AWS myaccesskey:I/jL9Lkq+n6DT0ZuLmK71B/wABQ=]), (date, [Tue, 07 Feb 2017 00:15:48 UTC])), null"),
				httpClient.getAndClearLog()
		)
	}

	@Test
	fun checkPut() = syncTest {
		s3["test/hello.json"].writeString("hello")
		Assert.assertEquals(
				listOf("PUT, https://test.s3.amazonaws.com/hello.json, Headers((Authorization, [AWS myaccesskey:lzucas2uhwPa2vsVJoRzta6RAtg=]), (content-length, [5]), (content-type, [application/json]), (date, [Tue, 07 Feb 2017 00:15:48 UTC]), (x-amz-acl, [private])), hello"),
				httpClient.getAndClearLog()
		)
	}

	@Test
	fun checkPut2() = syncTest {
		s3["test/hello.txt"].writeString("hello", MimeType.IMAGE_JPEG, S3.ACL.PUBLIC_READ)
		Assert.assertEquals(
				listOf("PUT, https://test.s3.amazonaws.com/hello.txt, Headers((Authorization, [AWS myaccesskey:DceOjup5BapxMUuh6Ww07viLyxg=]), (content-length, [5]), (content-type, [image/jpeg]), (date, [Tue, 07 Feb 2017 00:15:48 UTC]), (x-amz-acl, [public-read])), hello"),
				httpClient.getAndClearLog()
		)
	}
}