package com.soywiz.korio.vfs

import com.soywiz.korio.util.OS
import java.io.File
import java.util.*

object VfsUtil {
	fun parts(path: String): List<String> = path.split('/')

	fun normalize(path: String): String {
		val schemeIndex = path.indexOf(":")
		if (schemeIndex >= 0) {
			val take = if (path.substring(schemeIndex).startsWith("://")) 3 else 1
			return path.substring(0, schemeIndex + take) + normalize(path.substring(schemeIndex + take))
		} else {
			val path2 = path.replace('\\', '/')
			val out = LinkedList<String>()
			for (part in path2.split("/")) {
				when (part) {
					"", "." -> if (out.isEmpty()) out += "" else Unit
					".." -> if (out.isNotEmpty()) out.removeLast()
					else -> out += part
				}
			}
			return out.joinToString("/")
		}
	}

	fun combine(base: String, access: String): String = if (isAbsolute(access)) normalize(access) else normalize(base + "/" + access)

	fun lightCombine(base: String, access: String): String = if (base.isNotEmpty()) "$base/$access" else "$access"

	fun isAbsolute(base: String): Boolean {
		if (base.isEmpty()) return false
		val b = base.replace('\\', '/').substringBefore('/')
		if (b.isEmpty()) return true
		if (b.contains(':')) return true
		return false
	}

	fun normalizeAbsolute(path: String): String {
		//val res = path.replace('/', File.separatorChar).trim(File.separatorChar)
		//return if (OS.isUnix) "/$res" else res
		return path.replace('/', File.separatorChar)
	}
}