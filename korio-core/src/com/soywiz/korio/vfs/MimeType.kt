package com.soywiz.korio.vfs

class MimeType(val mime: String, val exts: List<String>) : Vfs.Attribute {
	companion object {
		val APPLICATION_OCTET_STREAM = MimeType("application/octet-stream", listOf("bin"))
		val APPLICATION_JSON = MimeType("application/json", listOf("json"))
		val IMAGE_JPEG = MimeType("image/jpeg", listOf("jpg", "jpeg"))
		val IMAGE_PNG = MimeType("image/png", listOf("png"))
		val TEXT_PLAIN = MimeType("text/plain", listOf("txt", "text"))
		val TEXT_CSS = MimeType("text/ss", listOf("css"))

		private val byExtensions = hashMapOf<String, MimeType>()

		fun register(mimeType: MimeType) {
			for (ext in mimeType.exts) byExtensions[ext] = mimeType
		}

		fun register(mime: String, vararg exsts: String) {
			register(MimeType(mime, exsts.map(String::toLowerCase)))
		}

		init {
			register(APPLICATION_OCTET_STREAM)
			register(TEXT_PLAIN)
			register(TEXT_CSS)
			register(APPLICATION_JSON)
			register(IMAGE_JPEG)
			register(IMAGE_PNG)
		}

		fun getByExtension(ext: String, default: MimeType = APPLICATION_OCTET_STREAM): MimeType = byExtensions[ext.toLowerCase()] ?: default
	}
}