package com.soywiz.coktvfs

data class VfsStat(
        val file: VfsFile,
        val exists: Boolean,
        val isDirectory: Boolean,
        val size: Long
) {
    val name: String get() = file.path
    val basename: String get() = file.basename
}