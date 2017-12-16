package com.soywiz.korio.net

import com.soywiz.korio.util.StrReader
import com.soywiz.korio.util.nullIf

data class URI private constructor(
	val isOpaque: Boolean,
	val scheme: String?,
	val userInfo: String?,
	val host: String?,
	val path: String,
	val query: String?
) {
	val user: String? get() = userInfo?.substringBefore(':')
	val password: String? get() = userInfo?.substringAfter(':')
	val isHierarchical get() = !isOpaque

	val fullUri: String by lazy {
		val out = StringBuilder()
		if (scheme != null) {
			out.append("$scheme:")
			if (!isOpaque) out.append("//")
		}
		if (userInfo != null) out.append("$userInfo@")
		if (host != null) out.append(host)
		out.append(path)
		if (query != null) out.append("?$query")
		out.toString()
	}

	val isAbsolute get() = (scheme != null)

	override fun toString(): String = fullUri
	fun toComponentString(): String {
		return "URI(" + listOf(::scheme, ::userInfo, ::host, ::path, ::query)
			.map { it.name to it.get() }
			.filter { it.second != null }
			.joinToString(", ") { "${it.first}=${it.second}" } + ")"
	}

	fun resolve(path: URI): URI = URI(resolve(this.fullUri, path.fullUri))

	companion object {
		operator fun invoke(
			scheme: String?,
			userInfo: String?,
			host: String?,
			path: String,
			query: String?,
			opaque: Boolean = false
		): URI = URI(opaque, scheme, userInfo, host, path, query)

		private val schemeRegex = Regex("\\w+:")

		operator fun invoke(uri: String): URI {
			val r = StrReader(uri)
			val schemeColon = r.tryRegex(schemeRegex)
			return when {
				schemeColon != null -> {
					val isHierarchical = r.tryLit("//") != null
					val nonScheme = r.readRemaining()
					val scheme = schemeColon.dropLast(1)
					val (nonQuery, query) = nonScheme.split('?', limit = 2).run { first() to getOrNull(1) }
					val (authority, path) = nonQuery.split('/', limit = 2).run { first() to (getOrNull(1) ?: "") }
					val (host, userInfo) = authority.split('@', limit = 2).reversed().run { first() to getOrNull(1) }
					URI(opaque = !isHierarchical, scheme = scheme, userInfo = userInfo, host = host.nullIf { isEmpty() }, path = if (path.isNotEmpty()) "/$path" else "", query = query)
				}
				else -> {
					val pathQuery = uri.split("?", limit = 2)
					val path = pathQuery.first()
					val query = if (pathQuery.size >= 2) pathQuery.last() else null
					URI(opaque = false, scheme = null, userInfo = null, host = null, path = path, query = query)
				}
			}
		}

		fun isAbsolute(uri: String): Boolean = StrReader(uri).tryRegex(schemeRegex) != null

		fun resolve(base: String, access: String): String = when {
			isAbsolute(access) -> access
			access.startsWith("/") -> URI(base).copy(path = access).fullUri
			else -> URI(base).run { copy(path = "/" + com.soywiz.korio.vfs.VfsUtil.normalize(this.path.substringBeforeLast('/') + "/" + access).trimStart('/')).fullUri }
		}
	}
}
