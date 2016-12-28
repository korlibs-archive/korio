package com.soywiz.korio.vfs

import java.util.*

data class VfsStat(
	val file: VfsFile,
	val exists: Boolean,
	val isDirectory: Boolean,
	val size: Long,
	val device: Long = -1L,
	val inode: Long = -1L,
	val mode: Int = 511,
	val owner: String = "nobody",
	val group: String = "nobody",
    val createTime: Long = 0L,
	val modifiedTime: Long = createTime,
	val lastAccessTime: Long = modifiedTime,
    val extraInfo: Any? = null
) : VfsNamed(file.path) {
	val createDate by lazy { Date(createTime) }
	val modifiedDate by lazy { Date(modifiedTime) }
	val lastAccessDate by lazy { Date(lastAccessTime) }
}