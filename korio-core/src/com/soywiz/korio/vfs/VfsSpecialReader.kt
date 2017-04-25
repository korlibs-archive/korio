package com.soywiz.korio.vfs

import java.util.*

abstract class VfsSpecialReader<T>(val clazz: Class<T>) {
	open val isAvailable: Boolean = true
	open fun readSpecial(vfs: Vfs, path: String): T = TODO()
}

val vfsSpecialReaders by lazy {
	ServiceLoader.load(VfsSpecialReader::class.java).map {
		it.clazz to it
	}.toMap()
}