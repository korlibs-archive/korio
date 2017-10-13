package com.soywiz.korio.vfs

import com.soywiz.korio.time.DateTime

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
) : VfsNamed(file.path)

//val VfsStat.createLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(createTime / 1000L, ((createTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)
//val VfsStat.modifiedLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(modifiedTime / 1000L, ((modifiedTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)
//val VfsStat.lastAccessLocalDate: LocalDateTime get() = LocalDateTime.ofEpochSecond(lastAccessTime / 1000L, ((lastAccessTime % 1_000L) * 1_000_000L).toInt(), ZoneOffset.UTC)

//val INIT = Unit.apply { println("UTC_OFFSET: $UTC_OFFSET")  }

val VfsStat.createDate: DateTime get() = DateTime.fromUnix(createTime)
val VfsStat.modifiedDate: DateTime get() = DateTime.fromUnix(modifiedTime)
val VfsStat.lastAccessDate: DateTime get() = DateTime.fromUnix(lastAccessTime)