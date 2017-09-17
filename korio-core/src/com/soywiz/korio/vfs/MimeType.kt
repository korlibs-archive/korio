package com.soywiz.korio.vfs

import com.soywiz.korio.util.allDeclaredFields
import java.lang.reflect.Modifier

class MimeType(val mime: String, val exts: List<String>) : Vfs.Attribute {
	companion object {
		@JvmStatic
		val APPLICATION_OCTET_STREAM = MimeType("application/octet-stream", listOf("bin"))
		@JvmStatic
		val APPLICATION_JSON = MimeType("application/json", listOf("json"))
		@JvmStatic
		val IMAGE_PNG = MimeType("image/png", listOf("png"))
		@JvmStatic
		val IMAGE_JPEG = MimeType("image/jpeg", listOf("jpg", "jpeg"))
		@JvmStatic
		val IMAGE_GIF = MimeType("image/gif", listOf("gif"))
		@JvmStatic
		val TEXT_HTML = MimeType("text/html", listOf("htm", "html"))
		@JvmStatic
		val TEXT_PLAIN = MimeType("text/plain", listOf("txt", "text"))
		@JvmStatic
		val TEXT_CSS = MimeType("text/css", listOf("css"))
		@JvmStatic
		val TEXT_JS = MimeType("application/javascript", listOf("js"))

		private val byExtensions = hashMapOf<String, MimeType>()

		fun register(mimeType: MimeType) {
			for (ext in mimeType.exts) byExtensions[ext] = mimeType
		}

		fun register(mime: String, vararg exsts: String) {
			register(MimeType(mime, exsts.map(String::toLowerCase)))
		}

		init {
			for (field in MimeType::class.java.allDeclaredFields) {
				if (Modifier.isStatic(field.modifiers) && (field.type == MimeType::class.java)) {
					register(field.get(null) as MimeType)
				}
			}
		}

		fun getByExtension(ext: String, default: MimeType = APPLICATION_OCTET_STREAM): MimeType = byExtensions[ext.toLowerCase()] ?: default
	}
}

fun VfsFile.mimeType() = MimeType.getByExtension(this.extensionLC)