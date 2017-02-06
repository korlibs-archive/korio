package com.soywiz.korio.ext.s3

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.net.http.createHttpClient
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.openAsync
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.vfs.*
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

suspend fun S3Vfs(region: String = System.getenv("AWS_DEFAULT_REGION") ?: "eu-west-1") = S3Client(region).root

class S3Client(val accessKey: String, val secretKey: String, val endpoint: String) : Vfs() {
	override val absolutePath: String = "https://$endpoint"
	val httpClient = createHttpClient()
	//val httpVfs = UrlVfs(absolutePath)

	//suspend fun get(bucket: String) = S3Bucket(this, bucket)

	companion object {
		suspend operator fun invoke(region: String = System.getenv("AWS_DEFAULT_REGION") ?: "eu-west-1"): S3Client {
			var accessKey = System.getenv("AWS_ACCESS_KEY_ID")?.trim()
			var secretKey = System.getenv("AWS_SECRET_KEY")?.trim()
			val userHome = System.getProperty("user.home")

			if (accessKey.isNullOrEmpty()) {
				val credentials = LocalVfs("$userHome/.aws")["credentials"].readString()
				accessKey = (Regex("aws_access_key_id\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
				secretKey = (Regex("aws_secret_access_key\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
			}

			return S3Client(accessKey ?: "", secretKey ?: "", "s3-$region.amazonaws.com")
		}
	}

	private fun normalizePath(path: String) = "/" + path.trim('/')

	class ACCESS(var text: String) : Vfs.Attribute {
		companion object {
			val PRIVATE = ACCESS("private")
			val PUBLIC_READ = ACCESS("public-read")
		}
	}
	class CONTENT_TYPE(var text: String) : Vfs.Attribute {
		companion object {
			val TEXT_PLAIN = CONTENT_TYPE("text/plain")
			val APPLICATION_OCTET_STREAM = CONTENT_TYPE("application/octet-stream")
		}
	}

	suspend override fun stat(path: String): VfsStat {
		val result = request(HttpClient.Method.HEAD, path)

		return if (result.success) {
			createExistsStat(path, isDirectory = true, size = result.headers["content-length"]?.toLongOrNull() ?: 0L)
		} else {
			createNonExistsStat(path)
		}
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		if (mode.write) invalidOp("Use put for writing")
		return request(
				HttpClient.Method.GET,
				path
		).content.toAsyncStream()
	}

	suspend override fun put(path: String, content: AsyncStream, attributes: List<Attribute>) {
		val access = attributes.get<ACCESS>()
		val contentType = attributes.get<CONTENT_TYPE>()

		request(
				HttpClient.Method.PUT,
				path,
				contentType = contentType?.text ?: "application/octet-stream",
				headers = mapOf(
						"x-amz-acl" to (access?.text ?: "private")
				)
		)
	}

	suspend fun request(method: HttpClient.Method, path: String, contentType: String = "", contentMd5: String = "", headers: Map<String, String> = mapOf(), content: ByteArray? = null): HttpClient.Response {
		val npath = normalizePath(path)
		return httpClient.request(
				method, absolutePath + npath,
				headers = genHeaders(method, npath, contentType, contentMd5, headers),
				content = content?.openAsync()
		)
	}

	// http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#ConstructingTheAuthenticationHeader
	private fun genHeaders(method: HttpClient.Method, path: String, contentType: String = "", contentMd5: String = "", headers: Map<String, String> = kotlin.collections.mapOf()): HttpClient.Headers {
		val awsAccessKey = accessKey
		val awsSecretKey = secretKey

		val _headers = hashMapOf<String, String>()
		val amzHeaders = kotlin.collections.hashMapOf<String, String>()

		fun addHeader(key: String, value: String) {
			val k = key.toLowerCase()
			val v = value.trim()
			_headers[k] = v
			if (k.startsWith("x-amz")) amzHeaders[k] = v
		}

		val date = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).format(java.util.Date())
		for (header in headers) addHeader(header.key, header.value)
		if (contentType.isNotEmpty()) addHeader("content-type", contentType)
		if (contentMd5.isNotEmpty()) addHeader("content-md5", contentMd5)

		addHeader("date", date)

		val canonicalizedAmzHeaders = amzHeaders.entries.sortedBy { it.key }.map { "${it.key}: ${it.value}\n" }.joinToString()
		val canonicalizedResource = path
		val toSign = method.name + "\n" + contentMd5 + "\n" + contentType + "\n" + date + "\n" + canonicalizedAmzHeaders + canonicalizedResource
		val signature = b64SignHmacSha1(awsSecretKey, toSign)
		val authorization = "AWS $awsAccessKey:$signature"
		addHeader("authorization", authorization)
		return HttpClient.Headers(_headers)
	}

	private fun b64SignHmacSha1(awsSecretKey: String, canonicalString: String): String {
		val signingKey = SecretKeySpec(awsSecretKey.toByteArray(Charsets.UTF_8), "HmacSHA1")
		return String(Base64.getEncoder().encode(Mac.getInstance("HmacSHA1").apply { init(signingKey) }.doFinal(canonicalString.toByteArray(Charsets.UTF_8))))
	}
}
