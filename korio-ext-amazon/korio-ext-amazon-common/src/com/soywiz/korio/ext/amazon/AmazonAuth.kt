package com.soywiz.korio.ext.amazon

import com.soywiz.korio.crypto.toBase64
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.util.substr
import com.soywiz.korio.util.toHexStringLower
import com.soywiz.korio.vfs.LocalVfs
import java.io.IOException
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AmazonAuth {
	class Credentials(val accessKey: String, val secretKey: String)

	suspend fun getCredentials(accessKey: String? = null, secretKey: String? = null): Credentials? {
		var finalAccessKey = accessKey
		var finalSecretKey = secretKey

		if (finalAccessKey.isNullOrEmpty()) {
			finalAccessKey = System.getenv("AWS_ACCESS_KEY_ID")?.trim()
			finalSecretKey = System.getenv("AWS_SECRET_KEY")?.trim()
		}

		if (finalAccessKey.isNullOrEmpty()) {
			try {
				val userHome = System.getProperty("user.home")
				val credentials = LocalVfs("$userHome/.aws")["credentials"].readString()
				finalAccessKey = (Regex("aws_access_key_id\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
				finalSecretKey = (Regex("aws_secret_access_key\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
			} catch (e: IOException) {
			}
		}
		return if (finalAccessKey != null && finalSecretKey != null) Credentials(finalAccessKey, finalSecretKey) else null
	}

	object V1 {
		val DATE_FORMAT = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }

		private fun macProcess(key: ByteArray, algo: String, data: ByteArray): ByteArray {
			return Mac.getInstance(algo).apply { init(SecretKeySpec(key, algo)) }.doFinal(data)
		}

		fun macProcessStringsB64(key: String, algo: String, data: String): String {
			return macProcess(key.toByteArray(), algo, data.toByteArray()).toBase64()
		}

		fun getAuthorization(accessKey: String, secretKey: String, method: Http.Method, cannonicalPath: String, headers: Http.Headers): String {
			val contentType = headers["content-type"] ?: ""
			val contentMd5 = headers["content-md5"] ?: ""
			val date = headers["date"] ?: ""

			val amzHeaders = hashMapOf<String, String>()

			for ((key, value) in headers) {
				val k = key.toLowerCase()
				val v = value.trim()
				if (k.startsWith("x-amz")) amzHeaders[k] = v
			}

			val canonicalizedAmzHeaders = amzHeaders.entries.sortedBy { it.key }.map { "${it.key}:${it.value}\n" }.joinToString()
			val canonicalizedResource = cannonicalPath
			val toSign = method.name + "\n" + contentMd5 + "\n" + contentType + "\n" + date + "\n" + canonicalizedAmzHeaders + canonicalizedResource
			val signature = macProcessStringsB64(secretKey, "HmacSHA1", toSign)
			return "AWS $accessKey:$signature"
		}
	}

	// http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
	object V4 {
		val DATE_FORMAT = SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }

		fun getSignedHeaders(headers: Http.Headers): String {
			return headers.toListGrouped().map { it.first.toLowerCase() to it.second.map(String::trim).joinToString(",") }.sortedBy { it.first }.map { it.first }.joinToString(";")
		}

		fun getCannonicalRequest(method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray): String {
			var CanonicalRequest = ""
			CanonicalRequest += method.name + "\n"
			CanonicalRequest += "/" + url.path.trim('/') + "\n"
			CanonicalRequest += (url.query ?: "") + "\n"
			for ((k, v) in headers.toListGrouped().map { it.first.toLowerCase() to it.second.map(String::trim).joinToString(",") }.sortedBy { it.first }) {
				CanonicalRequest += "$k:$v\n"
			}
			CanonicalRequest += "\n"
			CanonicalRequest += "${getSignedHeaders(headers)}\n"
			CanonicalRequest += SHA256(payload).toHexStringLower()
			return CanonicalRequest
		}

		fun getCannonicalRequestHash(method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray): ByteArray {
			return SHA256(getCannonicalRequest(method, url, headers, payload).toByteArray(Charsets.UTF_8))
		}

		fun SHA256(data: ByteArray): ByteArray {
			return MessageDigest.getInstance("SHA-256").digest(data);
		}

		fun HMAC(key: ByteArray, data: ByteArray): ByteArray {
			val algorithm = "HmacSHA256"
			val mac = Mac.getInstance(algorithm)
			mac.init(SecretKeySpec(key, algorithm))
			return mac.doFinal(data)
		}

		fun HmacSHA256(data: String, key: ByteArray): ByteArray {
			val algorithm = "HmacSHA256"
			val mac = Mac.getInstance(algorithm)
			mac.init(SecretKeySpec(key, algorithm))
			return mac.doFinal(data.toByteArray(charset("UTF8")))
		}

		fun getSignatureKey(key: String, dateStamp: String, regionName: String, serviceName: String): ByteArray {
			val kSecret = ("AWS4" + key).toByteArray(charset("UTF8"))
			val kDate = HmacSHA256(dateStamp, kSecret)
			val kRegion = HmacSHA256(regionName, kDate)
			val kService = HmacSHA256(serviceName, kRegion)
			val kSigning = HmacSHA256("aws4_request", kService)
			return kSigning
		}

		fun getStringToSign(method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray, region: String, service: String): String {
			val date = headers["X-Amz-Date"]!!
			val ddate = date.substr(0, 8)
			var StringToSign = ""
			StringToSign += "AWS4-HMAC-SHA256\n"
			StringToSign += date + "\n"
			StringToSign += "$ddate/$region/$service/aws4_request\n"
			StringToSign += AmazonAuth.V4.getCannonicalRequestHash(method, url, headers, payload).toHexStringLower()
			return StringToSign
		}

		fun getSignature(key: String, method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray, region: String, service: String): String {
			val date = headers["X-Amz-Date"]!!
			val ddate = date.substr(0, 8)
			val derivedSigningKey = getSignatureKey(key, ddate, region, service)
			val stringToSign = getStringToSign(method, url, headers, payload, region, service)
			//println("cannonicalRequest=${getCannonicalRequest(method, url, headers, payload)}")
			//println("derivedSigningKey=$derivedSigningKey")
			//println("stringToSign=$stringToSign")
			return HMAC(derivedSigningKey, stringToSign.toByteArray(Charsets.UTF_8)).toHexStringLower()
		}

		fun getAuthorization(accessKey: String, secretKey: String, method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray, region: String, service: String): String {
			val date = headers["X-Amz-Date"]!!
			val ddate = date.substr(0, 8)
			val signedHeaders = getSignedHeaders(headers)

			//AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20150830/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=5d672d79c15b13162d9279b0855cfba6789a8edb4c82c400e06b5924a6f2b5d7
			val signature = getSignature(secretKey, method, url, headers, payload, region, service)
			return "AWS4-HMAC-SHA256 Credential=$accessKey/$ddate/$region/$service/aws4_request, SignedHeaders=$signedHeaders, Signature=$signature"
		}

		fun signHeaders(accessKey: String, secretKey: String, method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray, region: String, service: String): Http.Headers {
			return headers.withReplaceHeaders(
					"Authorization" to getAuthorization(accessKey, secretKey, method, url, headers, payload, region, service)
			)
		}
	}
}