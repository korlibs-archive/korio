package com.soywiz.korio.vfs

enum class VfsOpenMode(val cmode: String, val write: Boolean, val createIfNotExists: Boolean = false, val truncate: Boolean = false) {
	READ("r", write = false),
	WRITE("rw", write = true, createIfNotExists = true),
	APPEND("a+", write = true, createIfNotExists = true),
	CREATE_OR_TRUNCATE("rw", write = true, createIfNotExists = true, truncate = true),
	CREATE("rw", write = true, createIfNotExists = true),
	CREATE_NEW("rw", write = true);

	companion object {
		fun fromString(str: String): VfsOpenMode {
			if ('r' in str) {
				return READ
			}
			TODO()
		}
	}
}