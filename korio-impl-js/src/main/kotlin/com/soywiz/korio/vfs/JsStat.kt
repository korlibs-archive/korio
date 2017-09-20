package com.soywiz.korio.vfs

import com.soywiz.korio.vfs.Vfs
import com.soywiz.korio.vfs.VfsStat

data class JsStat(val size: Double, var isDirectory: Boolean = false) {
	fun toStat(path: String, vfs: Vfs): VfsStat = vfs.createExistsStat(path, isDirectory = isDirectory, size = size.toLong())
}
