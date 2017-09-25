package com.soywiz.korio.vfs

import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.lang.KClass

abstract class VfsSpecialReader<T>(val clazz: KClass<T>) {
	open suspend fun readSpecial(vfs: Vfs, path: String): T = TODO("Not implemented VfsSpecialReader.readSpecial in ${this::class}")
}

val vfsSpecialReadersMap = lmapOf<KClass<*>, VfsSpecialReader<*>>()
fun registerVfsSpecialReader(sr: VfsSpecialReader<*>) {
	vfsSpecialReadersMap[sr.clazz] = sr
}