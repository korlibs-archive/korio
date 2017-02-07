package com.soywiz.korio.ext.s3

import com.soywiz.korio.async.syncTest
import com.soywiz.korio.net.http.LogHttpClient
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.MimeType
import com.soywiz.korio.vfs.VfsFile
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class S3ClientTest {
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
				listOf("GET, https://s3-demo.amazonaws.com/test/hello.txt, Headers((date, Tue, 07 Feb 2017 00:15:48 UTC), (authorization, AWS myaccesskey:I/jL9Lkq+n6DT0ZuLmK71B/wABQ=)), null"),
				httpClient.getAndClearLog()
		)
	}

	@Test
	fun checkPut() = syncTest {
		s3["test/hello.json"].writeString("hello")
		Assert.assertEquals(
				listOf("PUT, https://s3-demo.amazonaws.com/test/hello.json, Headers((date, Tue, 07 Feb 2017 00:15:48 UTC), (authorization, AWS myaccesskey:aU1zZtuG1Mj66uhzGrCFmQJ+D7Q=), (x-amz-acl, private), (content-type, application/json)), null"),
				httpClient.getAndClearLog()
		)

		s3["test/hello.txt"].writeString("hello", MimeType.IMAGE_JPEG, S3Client.ACL.PUBLIC_READ)
		Assert.assertEquals(
				listOf("PUT, https://s3-demo.amazonaws.com/test/hello.txt, Headers((date, Tue, 07 Feb 2017 00:15:48 UTC), (authorization, AWS myaccesskey:PyJ5Kx51hbY/P4p3nM1qgvqH0D4=), (x-amz-acl, public-read), (content-type, image/jpeg)), null"),
				httpClient.getAndClearLog()
		)
	}
}