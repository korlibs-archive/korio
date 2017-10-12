package com.soywiz.korio.ext.db

// @TODO: VfsFile <-> KeyStore
interface KeyStore {
	suspend fun get(key: String): String
	suspend fun set(key: String, value: String)
}