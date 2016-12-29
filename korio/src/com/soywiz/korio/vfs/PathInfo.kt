package com.soywiz.korio.vfs

class PathInfo(val fullpath: String) {
	val folder: String by lazy { fullpath.substringBeforeLast('/', "") }
	val basename: String by lazy { fullpath.substringAfterLast('/', fullpath) }
	val pathWithoutExtension: String by lazy { fullpath.substringBeforeLast('.', basename) }
	val basenameWithoutExtension: String by lazy { basename.substringBeforeLast('.', basename) }
	val extension: String by lazy { basename.substringAfterLast('.', "") }
}