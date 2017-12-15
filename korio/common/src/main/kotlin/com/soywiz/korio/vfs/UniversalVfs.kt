package com.soywiz.korio.vfs

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.URL

object UniversalVfs {
	@PublishedApi
	internal var schemaBuilders = hashMapOf<String, (URL) -> VfsFile>()

	fun registerSchema(schema: String, builder: (URL) -> VfsFile) {
		schemaBuilders[schema] = builder
	}

	inline fun keepSchemas(callback: () -> Unit) {
		val original = schemaBuilders.toMap()
		try {
			callback()
		} finally {
			schemaBuilders = HashMap(original)
		}
	}

	init {
		registerSchema("http") { UrlVfs(it.fullUrl) }
		registerSchema("https") { UrlVfs(it.fullUrl) }
		registerSchema("file") { rootLocalVfs[it.path] }
	}

	operator fun invoke(uri: String, base: VfsFile? = null): VfsFile {
		return when {
			URL.isAbsolute(uri) -> {
				val url = URL(uri)
				val builder = schemaBuilders[url.protocol]
				if (builder != null) {
					builder(url)
				} else {
					invalidOp("Unsupported schema '${url.protocol}'")
				}
			}
			(base != null) -> base[uri]
			else -> localCurrentDirVfs[uri]
		}
	}
}

// @TODO: Make general
val String.uniVfs get() = UniversalVfs(this)

fun String.uniVfs(base: VfsFile? = null): VfsFile = UniversalVfs(this, base)
