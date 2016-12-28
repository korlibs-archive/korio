package com.soywiz.korio.vfs

data class VfsStat(
	val file: VfsFile,
	val exists: Boolean,
	val isDirectory: Boolean,
	val size: Long
) : VfsNamed(file.path)