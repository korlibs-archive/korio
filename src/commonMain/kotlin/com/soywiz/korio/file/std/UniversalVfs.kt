package com.soywiz.korio.file.std

import com.soywiz.korio.error.*
import com.soywiz.korio.file.*
import com.soywiz.korio.net.*

object UniversalVfs {
	operator fun invoke(uri: String, providers: UniSchemaProviders, base: VfsFile? = null): VfsFile {
		return when {
			URL.isAbsolute(uri) -> {
				val uriUri = URL(uri)
				val builder = providers.providers[uriUri.scheme]
				if (builder != null) {
					builder.provider(uriUri)
				} else {
					invalidOp("Unsupported scheme '${uriUri.scheme}'")
				}
			}
			(base != null) -> base[uri]
			else -> localCurrentDirVfs[uri]
		}
	}
}

class UniSchema(val name: String, val provider: (URL) -> VfsFile)

class UniSchemaProviders(val providers: Map<String, UniSchema>) {
	constructor(providers: Iterable<UniSchema>) : this(providers.associateBy { it.name })
	constructor(vararg providers: UniSchema) : this(providers.associateBy { it.name })
}

val defaultUniSchema = UniSchemaProviders(
	UniSchema("http") { UrlVfs(it) },
	UniSchema("https") { UrlVfs(it) },
	UniSchema("file") { rootLocalVfs[it.path] }
)

operator fun UniSchemaProviders.plus(other: UniSchemaProviders) = UniSchemaProviders(this.providers + other.providers)
operator fun UniSchemaProviders.plus(other: UniSchema) = UniSchemaProviders(this.providers + mapOf(other.name to other))

// @TODO: Make general
val String.uniVfs get() = UniversalVfs(this, defaultUniSchema)

fun String.uniVfs(providers: UniSchemaProviders, base: VfsFile? = null): VfsFile =
	UniversalVfs(this, providers, base)
