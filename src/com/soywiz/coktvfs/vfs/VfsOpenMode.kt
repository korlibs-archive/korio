package com.soywiz.coktvfs.vfs

enum class VfsOpenMode {
	READ,
	WRITE,
	APPEND,
	TRUNCATE_EXISTING,
	CREATE,
	CREATE_NEW,
}