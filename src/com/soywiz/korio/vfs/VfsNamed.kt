package com.soywiz.korio.vfs

open class VfsNamed(val path: String) {
	val fullname: String get() = path
	val basename: String by lazy { path.substringAfterLast('/') }
	val nameWithoutExtension: String by lazy { path.substringBeforeLast('.', path) }
	val extension: String by lazy { basename.substringAfterLast('.') }
}