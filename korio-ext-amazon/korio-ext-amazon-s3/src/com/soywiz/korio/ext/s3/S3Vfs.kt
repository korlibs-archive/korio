package com.soywiz.korio.ext.s3

import com.soywiz.korio.lang.Environment
import com.soywiz.korio.net.http.HttpClient
import com.soywiz.korio.util.TimeProvider
import com.soywiz.korio.vfs.VfsFile

suspend fun S3Vfs(
	region: String = Environment["AWS_DEFAULT_REGION"] ?: "eu-west-1",
	accessKey: String? = null, secretKey: String? = null,
	httpClient: HttpClient = HttpClient(),
	timeProvider: TimeProvider = TimeProvider()
): VfsFile = S3(region, accessKey, secretKey, httpClient, timeProvider).root
