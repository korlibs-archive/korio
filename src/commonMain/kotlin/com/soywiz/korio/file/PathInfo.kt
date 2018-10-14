package com.soywiz.korio.file

import com.soywiz.korio.lang.*
import com.soywiz.korio.net.*

// @TODO: inline classes. Once done PathInfoExt won't be required to do clean allocation-free stuff.
class PathInfo(override val fullPath: String) : Path

val String.pathInfo get() = PathInfo(this)

// @TODO: Kotlin 1.3 inline classes
object PathInfoExt {
	/**
	 * /path\to/file.ext -> /path/to/file.ext
	 */
	val String.fullpathNormalized: String get() = this.replace('\\', '/')

	/**
	 * /path\to/file.ext -> /path/to
	 */
	val String.folder: String get() = this.substring(0, fullpathNormalized.lastIndexOfOrNull('/') ?: 0)

	/**
	 * /path\to/file.ext -> /path/to/
	 */
	val String.folderWithSlash: String
		get() = this.substring(0, fullpathNormalized.lastIndexOfOrNull('/')?.plus(1) ?: 0)

	/**
	 * /path\to/file.ext -> file.ext
	 */
	val String.basename: String get() = fullpathNormalized.substringAfterLast('/')

	/**
	 * /path\to/file.ext -> /path\to/file
	 */
	val String.fullPathWithoutExtension: String
		get() = run {
			val startIndex = fullpathNormalized.lastIndexOfOrNull('/')?.plus(1) ?: 0
			this.substring(0, fullpathNormalized.indexOfOrNull('.', startIndex) ?: fullpathNormalized.length)
		}

	/**
	 * /path\to/file.ext -> /path\to/file.newext
	 */
	fun String.fullPathWithExtension(ext: String): String =
		if (ext.isEmpty()) fullPathWithoutExtension else "$fullPathWithoutExtension.$ext"

	/**
	 * /path\to/file.1.ext -> file.1
	 */
	val String.basenameWithoutExtension: String get() = basename.substringBeforeLast('.', basename)

	/**
	 * /path\to/file.1.ext -> file
	 */
	val String.basenameWithoutCompoundExtension: String get() = basename.substringBefore('.', basename)

	/**
	 * /path\to/file.1.ext -> /path\to/file.1
	 */
	val String.fullnameWithoutExtension: String get() = "$folderWithSlash$basenameWithoutExtension"

	/**
	 * /path\to/file.1.ext -> file
	 */
	val String.fullnameWithoutCompoundExtension: String get() = "$folderWithSlash$basenameWithoutCompoundExtension"

	/**
	 * /path\to/file.1.ext -> file.1.newext
	 */
	fun String.basenameWithExtension(ext: String): String =
		if (ext.isEmpty()) basenameWithoutExtension else "$basenameWithoutExtension.$ext"

	/**
	 * /path\to/file.1.ext -> file.newext
	 */
	fun String.basenameWithCompoundExtension(ext: String): String =
		if (ext.isEmpty()) basenameWithoutCompoundExtension else "$basenameWithoutCompoundExtension.$ext"

	/**
	 * /path\to/file.1.EXT -> EXT
	 */
	val String.extension: String get() = basename.substringAfterLast('.', "")

	/**
	 * /path\to/file.1.EXT -> ext
	 */
	val String.extensionLC: String get() = extension.toLowerCase()

	/**
	 * /path\to/file.1.EXT -> 1.EXT
	 */
	val String.compoundExtension: String get() = basename.substringAfter('.', "")

	/**
	 * /path\to/file.1.EXT -> 1.ext
	 */
	val String.compoundExtensionLC: String get() = compoundExtension.toLowerCase()

	/**
	 * /path\to/file.1.jpg -> MimeType("image/jpeg", listOf("jpg", "jpeg"))
	 */
	val String.mimeTypeByExtension get() = MimeType.getByExtension(extensionLC)

	/**
	 * /path\to/file.1.ext -> listOf("", "path", "to", "file.1.ext")
	 */
	fun String.getPathComponents(): List<String> = fullpathNormalized.split('/')

	/**
	 * /path\to/file.1.ext -> listOf("/path", "/path/to", "/path/to/file.1.ext")
	 */
	fun String.getPathFullComponents(): List<String> {
		val out = arrayListOf<String>()
		for (n in fullpathNormalized.indices) {
			when (fullpathNormalized[n]) {
				'/', '\\' -> {
					out += fullpathNormalized.substring(0, n)
				}
			}
		}
		out += fullpathNormalized
		return out
	}

	/**
	 * /path\to/file.1.ext -> /path\to/file.1.ext
	 */
	val String.fullName: String get() = this
}

inline fun <T> PathInfo(callback: PathInfoExt.() -> T): T = callback(PathInfoExt)

interface Path {
	val fullPath: String
}

val Path.fullpathNormalized: String get() = PathInfo { fullPath.fullpathNormalized }
val Path.folder: String get() = PathInfo { fullPath.folder }
val Path.folderWithSlash: String get() = PathInfo { fullPath.folderWithSlash }
val Path.basename: String get() = PathInfo { fullPath.basename }
val Path.pathWithoutExtension: String get() = PathInfo { fullPath.fullPathWithoutExtension }
fun Path.fullPathWithExtension(ext: String): String =
	PathInfo { fullPath.fullPathWithExtension(ext) }

val Path.fullnameWithoutExtension: String get() = PathInfo { fullPath.fullnameWithoutExtension }
val Path.basenameWithoutExtension: String get() = PathInfo { fullPath.basenameWithoutExtension }
val Path.fullnameWithoutCompoundExtension: String get() = PathInfo { fullPath.fullnameWithoutCompoundExtension }
val Path.basenameWithoutCompoundExtension: String get() = PathInfo { fullPath.basenameWithoutCompoundExtension }
fun Path.basenameWithExtension(ext: String): String =
	PathInfo { fullPath.basenameWithExtension(ext) }

fun Path.basenameWithCompoundExtension(ext: String): String =
	PathInfo { fullPath.basenameWithCompoundExtension(ext) }

val Path.extension: String get() = PathInfo { fullPath.extension }
val Path.extensionLC: String get() = PathInfo { fullPath.extensionLC }
val Path.compoundExtension: String get() = PathInfo { fullPath.compoundExtension }
val Path.compoundExtensionLC: String get() = PathInfo { fullPath.compoundExtensionLC }
val Path.mimeTypeByExtension: MimeType get() = PathInfo { fullPath.mimeTypeByExtension }
fun Path.getPathComponents(): List<String> =
	PathInfo { fullPath.getPathComponents() }

fun Path.getPathFullComponents(): List<String> =
	PathInfo { fullPath.getPathFullComponents() }

val Path.fullname: String get() = fullPath

open class VfsNamed(override val fullPath: String) : Path
