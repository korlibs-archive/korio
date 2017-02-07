package com.soywiz.korio.ext.s3

import com.soywiz.korio.crypto.toBase64
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.*
import com.soywiz.korio.vfs.MimeType
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#ConstructingTheAuthenticationHeader
class S3(val accessKey: String, val secretKey: String, val endpoint: String, val httpClient: HttpClient, val timeProvider: TimeProvider) : Vfs() {
	override fun getAbsolutePath(path: String) = parsePath(path).absolutePath

	override val supportedAttributeTypes = listOf(ACL::class.java, MimeType::class.java)

	//val httpVfs = UrlVfs(absolutePath)

	//suspend fun get(bucket: String) = S3Bucket(this, bucket)

	companion object {
		suspend operator fun invoke(region: String = System.getenv("AWS_DEFAULT_REGION") ?: "eu-west-1", accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = HttpClient(), timeProvider: TimeProvider = TimeProvider()): S3 {
			var finalAccessKey = accessKey
			var finalSecretKey = secretKey

			if (finalAccessKey.isNullOrEmpty()) {
				finalAccessKey = System.getenv("AWS_ACCESS_KEY_ID")?.trim()
				finalSecretKey = System.getenv("AWS_SECRET_KEY")?.trim()
			}

			if (accessKey.isNullOrEmpty()) {
				val userHome = System.getProperty("user.home")
				val credentials = LocalVfs("$userHome/.aws")["credentials"].readString()
				finalAccessKey = (Regex("aws_access_key_id\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
				finalSecretKey = (Regex("aws_secret_access_key\\s+=\\s+(.*)").find(credentials)?.groupValues?.getOrElse(1) { "" } ?: "").trim()
			}

			return S3(finalAccessKey ?: "", finalSecretKey ?: "", "s3-$region.amazonaws.com", httpClient, timeProvider)
		}
	}

	private fun normalizePath(path: String) = "/" + path.trim('/')

	class ACL(var text: String) : Vfs.Attribute {
		companion object {
			val PRIVATE = ACL("private")
			val PUBLIC_READ = ACL("public-read")
			val PUBLIC_READ_WRITE = ACL("public-read-write")
			val AWS_EXEC_READ = ACL("aws-exec-read")
			val AUTHENTICATED_READ = ACL("authenticated-read")
			val BUCKET_OWNER_READ = ACL("bucket-owner-read")
			val BUCKET_OWNER_FULL_CONTROL = ACL("bucket-owner-full-control")
			val LOG_DELIVERY_WRITE = ACL("log-delivery-write")
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
		return request(HttpClient.Method.GET, path).content.toAsyncStream()
	}

	suspend override fun put(path: String, content: AsyncStream, attributes: List<Attribute>) {
		val access = attributes.get<ACL>() ?: ACL.PRIVATE
		val contentType = attributes.get<MimeType>() ?: PathInfo(path).mimeTypeByExtension
		//val contentLength = content.getLength()

		request(
				HttpClient.Method.PUT,
				path,
				contentType = contentType.mime,
				headers = mapOf(
						//"content-length" to "${content.getLength()}", // @Kotlin error: java.lang.VerifyError: Bad type on operand stack
						//"content-length" to "$contentLength",
						"x-amz-acl" to access.text
				),
				content = content
		)
	}

	suspend fun request(method: HttpClient.Method, path: String, contentType: String = "", contentMd5: String = "", headers: Map<String, String> = mapOf(), content: AsyncStream? = null): HttpClient.Response {
		val npath = parsePath(path)
		return httpClient.request(
				method, npath.absolutePath,
				headers = genHeaders(method, npath, contentType, contentMd5, headers),
				content = content
		)
	}

	data class ParsedPath(val bucket: String, val key: String) {
		val absolutePath = "https://$bucket.s3.amazonaws.com/$key"
		val cannonical = "/$bucket/$key"
	}

	private fun parsePath(path: String): ParsedPath {
		val npath = path.trim('/')
		val parts = npath.split('/', limit = 2)
		return ParsedPath(parts[0].trim('/'), parts[1].trim('/'))
	}

	private fun genHeaders(method: HttpClient.Method, path: ParsedPath, contentType: String = "", contentMd5: String = "", headers: Map<String, String> = kotlin.collections.mapOf()): HttpClient.Headers {
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

		val date = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(java.util.Date(timeProvider.currentTimeMillis()))
		for (header in headers) addHeader(header.key, header.value)
		if (contentType.isNotEmpty()) addHeader("content-type", contentType)
		if (contentMd5.isNotEmpty()) addHeader("content-md5", contentMd5)

		addHeader("date", date)

		val canonicalizedAmzHeaders = amzHeaders.entries.sortedBy { it.key }.map { "${it.key}: ${it.value}\n" }.joinToString()
		val canonicalizedResource = path.cannonical
		val toSign = method.name + "\n" + contentMd5 + "\n" + contentType + "\n" + date + "\n" + canonicalizedAmzHeaders + canonicalizedResource
		val signature = b64SignHmacSha1(awsSecretKey, toSign)
		val authorization = "AWS $awsAccessKey:$signature"
		addHeader("authorization", authorization)
		return HttpClient.Headers(_headers)
	}

	private fun b64SignHmacSha1(awsSecretKey: String, canonicalString: String): String {
		val signingKey = SecretKeySpec(awsSecretKey.toByteArray(Charsets.UTF_8), "HmacSHA1")
		return Mac.getInstance("HmacSHA1").apply { init(signingKey) }.doFinal(canonicalString.toByteArray(Charsets.UTF_8)).toBase64()
	}
}
