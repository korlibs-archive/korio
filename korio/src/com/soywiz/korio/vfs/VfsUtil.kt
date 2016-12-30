package com.soywiz.korio.vfs

import com.soywiz.korio.util.OS
import java.io.File
import java.util.*

object VfsUtil {
	fun parts(path: String): List<String> = path.split('/')

	fun normalize(path: String): String {
		var path2 = path.replace('\\', '/')
		while (path2.startsWith("/")) path2 = path2.substring(1)
		val out = LinkedList<String>()
		for (part in path2.split("/")) {
			when (part) {
				"", "." -> Unit
				".." -> if (out.isNotEmpty()) out.removeLast()
				else -> out += part
			}
		}
		return out.joinToString("/")
	}

	fun combine(base: String, access: String): String = if (isAbsolute(access)) normalize(access) else normalize(base + "/" + access)

	fun isAbsolute(base: String): Boolean {
		if (base.isNotEmpty() && (base[0] == '/' || base[0] == '\\')) return true
		if (base.length >= 3 && (base[1] == ':' && (base[2] == '\\' || base[2] == '/'))) return true
		return false
	}

	fun normalizeAbsolute(path: String): String {
		val res = path.replace('/', File.separatorChar).trim(File.separatorChar)
		return if (OS.isUnix) "/$res" else res
	}
}