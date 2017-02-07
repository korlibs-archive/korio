package com.soywiz.korio.ext.amazon

import com.soywiz.korio.net.http.Http
import com.soywiz.korio.util.toHexString
import com.soywiz.korio.util.toHexStringLower
import org.junit.Assert
import org.junit.Test
import java.net.URL

class AmazonAuthTest {
	val method = Http.Method.GET
	val accessKey = "AKIDEXAMPLE"
	val secretKey = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY"
	val url = URL("https://iam.amazonaws.com/?Action=ListUsers&Version=2010-05-08")
	val region = "us-east-1"
	val service = "iam"
	val headers = Http.Headers(
			"Host" to "iam.amazonaws.com",
			"Content-Type" to "application/x-www-form-urlencoded; charset=utf-8",
			"X-Amz-Date" to "20150830T123600Z"
	)
	val payload = byteArrayOf()

	// http://docs.aws.amazon.com/general/latest/gr/sigv4-create-canonical-request.html
	// Task 1: Create a Canonical Request for Signature Version 4
	@Test
	fun task1Test() {
		val expected = listOf(
				"GET",
				"/",
				"Action=ListUsers&Version=2010-05-08",
				"content-type:application/x-www-form-urlencoded; charset=utf-8",
				"host:iam.amazonaws.com",
				"x-amz-date:20150830T123600Z",
				"",
				"content-type;host;x-amz-date",
				"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
		).joinToString("\n")

		val request = AmazonAuth.V4.getCannonicalRequest(method, url, headers, payload)

		Assert.assertEquals(expected, request)
		Assert.assertEquals(
				"f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59",
				AmazonAuth.V4.SHA256(request.toByteArray(Charsets.UTF_8)).toHexStringLower()
		)
	}

	// http://docs.aws.amazon.com/general/latest/gr/sigv4-create-string-to-sign.html
	// Task 2: Create a String to Sign for Signature Version 4
	@Test
	fun task2Test() {
		Assert.assertEquals(
				listOf(
						"AWS4-HMAC-SHA256",
						"20150830T123600Z",
						"20150830/us-east-1/iam/aws4_request",
						"f536975d06c0309214f805bb90ccff089219ecd68b2577efef23edd43b7e1a59"
				).joinToString("\n"),
				AmazonAuth.V4.getStringToSign(method, url, headers, payload, region, service)
		)
	}

	// http://docs.aws.amazon.com/general/latest/gr/sigv4-calculate-signature.html
	// Task 3: Calculate the Signature for AWS Signature Version 4
	@Test
	fun task3Test() {
		Assert.assertEquals(
				"f4780e2d9f65fa895f9c67b32ce1baf0b0d8a43505a000a1a9e090d414db404d",
				AmazonAuth.V4.getSignatureKey(secretKey, "20120215", "us-east-1", "iam").toHexString().toLowerCase()
		)
	}

	@Test
	fun task4Test() {
		Assert.assertEquals(
				"AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7",
				AmazonAuth.V4.getAuthorization(accessKey, secretKey, method, url, headers, payload, region, service)
		)
	}
}