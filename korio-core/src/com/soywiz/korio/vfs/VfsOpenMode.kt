package com.soywiz.korio.vfs

enum class VfsOpenMode(val cmode: String, val createIfNotExists: Boolean = false, val truncate: Boolean = false) {
	READ("r"),
	WRITE("rw", createIfNotExists = true),
	APPEND("a+", createIfNotExists = true),
	CREATE_OR_TRUNCATE("rw", createIfNotExists = true, truncate = true),
	CREATE("rw", createIfNotExists = true),
	CREATE_NEW("rw");

	companion object {
		fun fromString(str: String): VfsOpenMode {
			if ('r' in str) {
				return READ
			}
			TODO()
		}
	}
}