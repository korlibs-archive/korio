package com.soywiz.korio.lang

import com.soywiz.korio.vfs.VfsUtil

class URL(val url: String) {
	private val partsA = url.split("://", limit = 2)
	private val partsB = partsA.getOrElse(1) { "" }.split("/", limit = 2)
	private val partsC = partsB.getOrElse(1) { "" }.split("?", limit = 2)
	private val fullHost = partsB.getOrElse(0) { "" }
	private val fullPath = partsC.getOrElse(0) { "" }
	private val queryString = partsC.getOrElse(1) { "" }

	private val fullHostParts = fullHost.split("@", limit = 2)
	private val userPass = if (fullHostParts.size == 2) fullHostParts[0] else ""
	private val userPassParts = userPass.split(':', limit = 2)

	val protocol: String = partsA.getOrElse(0) { "" }
	val user = userPassParts.getOrElse(0) { "" }
	val password = userPassParts.getOrElse(1) { "" }
	val host = fullHostParts.last()
	val path = "/" + fullPath
	val query = queryString

	override fun toString(): String = url

}

object URIUtils {
	fun isAbsolute(base: String): Boolean = base.contains("://")
	fun resolve(base: String, access: String): String = if (isAbsolute(access)) {
		access
	} else {
		VfsUtil.combine(base, access)
	}
}