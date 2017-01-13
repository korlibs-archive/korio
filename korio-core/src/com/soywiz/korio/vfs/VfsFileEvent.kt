package com.soywiz.korio.vfs

data class VfsFileEvent(val kind: Kind, val file: VfsFile, val other: VfsFile? = null) {
	enum class Kind { DELETED, MODIFIED, CREATED, RENAMED }

	override fun toString() = if (other != null) "$kind($file, $other)" else "$kind($file)"
}