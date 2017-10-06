package com.soywiz.korio.ext.s3

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.ext.amazon.AmazonAuth
import com.soywiz.korio.lang.Environment
import com.soywiz.korio.net.http.Http
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.stream.AsyncInputStream
import com.soywiz.korio.stream.AsyncStream
import com.soywiz.korio.stream.toAsyncStream
import com.soywiz.korio.time.UTCDate
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.*

// http://docs.amazonwebservices.com/AmazonS3/latest/dev/RESTAuthentication.html#ConstructingTheAuthenticationHeader
class S3(val credentials: AmazonAuth.Credentials?, val endpoint: String, val httpClient: HttpClient, val timeProvider: TimeProvider) : Vfs() {
	override fun getAbsolutePath(path: String) = parsePath(path).absolutePath
	override val supportedAttributeTypes = listOf(ACL::class, MimeType::class)

	companion object {
		suspend operator fun invoke(region: String = Environment.get("AWS_DEFAULT_REGION") ?: "eu-west-1", accessKey: String? = null, secretKey: String? = null, httpClient: HttpClient = HttpClient(), timeProvider: TimeProvider = TimeProvider()): S3 {
			return S3(AmazonAuth.getCredentials(accessKey, secretKey), "s3-$region.amazonaws.com", httpClient, timeProvider)
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
		val result = request(Http.Method.HEAD, path)

		return if (result.success) {
			createExistsStat(path, isDirectory = true, size = result.headers.getFirst("content-length")?.toLongOrNull() ?: 0L)
		} else {
			createNonExistsStat(path)
		}
	}

	suspend override fun open(path: String, mode: VfsOpenMode): AsyncStream {
		if (mode.write) invalidOp("Use put for writing")
		return request(Http.Method.GET, path).content.toAsyncStream()
	}

	suspend override fun put(path: String, content: AsyncInputStream, attributes: List<Attribute>): Long {
		if (content !is AsyncStream) invalidOp("S3.put requires AsyncStream")
		val access = attributes.get<ACL>() ?: ACL.PRIVATE
		val contentType = attributes.get<MimeType>() ?: PathInfo(path).mimeTypeByExtension
		//val contentLength = content.getAvailable()

		request(
			Http.Method.PUT,
			path,
			contentType = contentType.mime,
			headers = Http.Headers(
				"content-type" to contentType.mime,
				"content-length" to "${content.getLength()}", // @TODO: @KOTLIN @BUG error: java.lang.VerifyError: Bad type on operand stack
				//"content-length" to "$contentLength",
				"x-amz-acl" to access.text
			),
			content = content
		)

		return content.getLength()
	}

	suspend fun request(method: Http.Method, path: String, contentType: String = "", contentMd5: String = "", headers: Http.Headers = Http.Headers(), content: AsyncStream? = null): HttpClient.Response {
		val npath = parsePath(path)
		return httpClient.request(
			method, npath.absolutePath,
			headers = genHeaders(method, npath, headers.withReplaceHeaders(
				"date" to AmazonAuth.V1.DATE_FORMAT.format(UTCDate(timeProvider.currentTimeMillis()))
			)),
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
		return ParsedPath(parts[0].trim('/'), parts.getOrElse(1) { "" }.trim('/'))
	}

	suspend private fun genHeaders(method: Http.Method, path: ParsedPath, headers: Http.Headers = Http.Headers()): Http.Headers {
		if (credentials != null) {
			return headers.withReplaceHeaders("Authorization" to AmazonAuth.V1.getAuthorization(credentials.accessKey, credentials.secretKey, method, path.cannonical, headers))
		} else {
			return headers
		}
	}
}
