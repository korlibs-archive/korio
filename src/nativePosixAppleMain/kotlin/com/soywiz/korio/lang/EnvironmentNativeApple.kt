package com.soywiz.korio.lang

actual object Environment {
	val allEnvs: Map<String, String> = platform.Foundation.NSProcessInfo.processInfo.environment.map { it.key.toString() to it.value.toString() }.toMap()
	val allEnvsUpper: Map<String, String> = allEnvs.map { it.key.toUpperCase() to it.value }.toMap()

	//actual operator fun get(key: String): String? = platform.posix.getenv(key)?.toKString()
	actual operator fun get(key: String): String? = allEnvsUpper[key.toUpperCase()]
	actual fun getAll() = allEnvs
}
