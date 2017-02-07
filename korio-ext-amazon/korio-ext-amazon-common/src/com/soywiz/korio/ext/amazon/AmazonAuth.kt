package com.soywiz.korio.ext.amazon

import com.soywiz.korio.crypto.toBase64
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.util.substr
import com.soywiz.korio.util.toHexStringLower
import java.net.URL
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AmazonAuth {
	private fun macProcess(key: ByteArray, algo: String, data: ByteArray): ByteArray {
		return Mac.getInstance(algo).apply { init(SecretKeySpec(key, algo)) }.doFinal(data)
	}

	fun macProcessStringsB64(key: String, algo: String, data: String): String {
		return macProcess(key.toByteArray(), algo, data.toByteArray()).toBase64()
	}

	fun genAwsAuthorization(accessKey: String, secretKey: String, canonicalString: String): String {
		val signature = macProcessStringsB64(secretKey, "HmacSHA1", canonicalString)
		return "AWS $accessKey:$signature"
	}

	fun genAwsSign4Sha256() {

	}

	// http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html
	object V4 {
		fun getSignedHeaders(headers: Http.Headers): String {
			return headers.items.toList().map { it.first.toLowerCase() to it.second.map(String::trim).joinToString(",") }.sortedBy { it.first }.map { it.first }.joinToString(";")
		}

		fun getCannonicalRequest(method: Http.Method, url: URL, headers: Http.Headers, payload: ByteArray): String {
			var CanonicalRequest = ""
			CanonicalRequest += method.name + "\n"
			CanonicalRequest += url.path + "\n"
			CanonicalRequest += url.query + "\n"
			for ((k, v) in headers.items.toList().map { it.first.toLowerCase() to it.second.map(String::trim).joinToString(",") }.sortedBy { it.first }) {
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
	}
}