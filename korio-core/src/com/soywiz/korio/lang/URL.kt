package com.soywiz.korio.lang

import com.soywiz.korio.vfs.VfsUtil

class URL(val url: String)

object URIUtils {
	fun isAbsolute(base: String): Boolean = VfsUtil.isAbsolute(base)
	fun resolve(base: String, access: String) = VfsUtil.combine(base, access)
}