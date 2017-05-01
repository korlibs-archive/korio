package com.soywiz.korio.vfs

open class VfsNamed(val path: String) {
	val fullname: String get() = path
	val pathInfo: PathInfo by lazy { PathInfo(path) }
	val basename: String get() = pathInfo.basename
	val fullnameWithoutExtension: String get() = pathInfo.fullnameWithoutExtension
	val basenameWithoutExtension: String get() = pathInfo.basenameWithoutExtension

	val fullnameWithoutCompoundExtension: String get() = pathInfo.fullnameWithoutCompoundExtension
	val basenameWithoutCompoundExtension: String get() = pathInfo.basenameWithoutCompoundExtension

	val extension: String get() = pathInfo.extension
	val extensionLC: String get() = pathInfo.extensionLC
	val compoundExtension: String get() = pathInfo.compoundExtension
	val compoundExtensionLC: String get() = pathInfo.compoundExtensionLC
}