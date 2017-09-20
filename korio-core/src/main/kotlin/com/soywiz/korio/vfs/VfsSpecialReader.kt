package com.soywiz.korio.vfs

import com.soywiz.korio.lang.KClass

abstract class VfsSpecialReader<T>(val clazz: KClass<T>) {
	open suspend fun readSpecial(vfs: Vfs, path: String): T = TODO("Not implemented VfsSpecialReader.readSpecial in ${this::class}")
}

val vfsSpecialReadersMap by lazy { vfsSpecialReaders.map { it.clazz to it }.toMap() }
header val vfsSpecialReaders: List<VfsSpecialReader<*>>