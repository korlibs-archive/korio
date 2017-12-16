package com.soywiz.korio.lang

import com.soywiz.korio.vfs.VfsUtil

@Deprecated("Use com.soywiz.korio.net.URI instead")
data class URL private constructor(
	val dummy: Boolean,
	val protocol: String,
	val auth: String,
	val host: String,
	val path: String,
	val query: String
) {
	val user: String get() = auth.split(':').getOrElse(0) { "" }
	val password: String get() = auth.split(':').getOrElse(1) { "" }

	private val str: String by lazy {
		val out = StringBuilder()
		out.append("$protocol://")
		if (auth.isNotEmpty()) out.append("$auth@")
		out.append(host)
		out.append('/')
		out.append(path.trimStart('/'))
		if (query.isNotEmpty()) out.append("?$query")
		out.toString()
	}

	val fullUrl: String get() = str

	override fun toString(): String = str

	fun toComponentString(): String = "URL(protocol=$protocol, auth=$auth, host=$host, path=$path, query=$query)"

	//fun withProtocol(protocol: String) = copy(protocol = protocol)
	//fun withHost(host: String) = copy(host = host)
	//fun withPath(path: String) = copy(path = path)
	//fun withQuery(query: String) = copy(query = query)
	//fun withAuth(auth: String) = copy(auth = auth)

	companion object {
		operator fun invoke(
			protocol: String,
			auth: String,
			host: String,
			path: String,
			query: String
		): URL = URL(false, protocol, auth, host, path, query)

		operator fun invoke(url: String): URL {
			val partsA = url.split("://", limit = 2)
			val partsB = partsA.getOrElse(1) { "" }.split("/", limit = 2)
			val partsC = partsB.getOrElse(1) { "" }.split("?", limit = 2)
			val fullHost = partsB.getOrElse(0) { "" }
			val fullPath = partsC.getOrElse(0) { "" }
			val queryString = partsC.getOrElse(1) { "" }

			val fullHostParts = fullHost.split("@", limit = 2)
			val userPassStr = if (fullHostParts.size == 2) fullHostParts[0] else ""

			return URL(
				protocol = partsA.getOrElse(0) { "" },
				auth = userPassStr,
				host = fullHostParts.last(),
				path = "/" + fullPath.trimStart('/'),
				query = queryString
			)
		}

		fun isAbsolute(base: String): Boolean = base.contains("://") || base.startsWith("/")

		fun resolve(base: String, access: String): String = when {
			isAbsolute(access) -> access
			else -> URL(base + "/" + access).run { this.copy(path = VfsUtil.normalize(this.path)).toString() }
		}
	}
}

object URIUtils {
	@Deprecated("", ReplaceWith("URL.isAbsolute(base)"))
	fun isAbsolute(base: String): Boolean = URL.isAbsolute(base)

	@Deprecated("", ReplaceWith("URL.resolve(base, access)"))
	fun resolve(base: String, access: String): String = URL.resolve(base, access)
}