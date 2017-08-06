package com.soywiz.korio.vfs

import com.soywiz.korio.service.Services

abstract class VfsSpecialReader<T>(val clazz: Class<T>) : Services.Impl() {
	open suspend fun readSpecial(vfs: Vfs, path: String): T = TODO("Not implemented VfsSpecialReader.readSpecial in ${this.javaClass}")
}

val vfsSpecialReaders by lazy {
	Services.loadList(VfsSpecialReader::class.java).map { it.clazz to it }.toMap()
}