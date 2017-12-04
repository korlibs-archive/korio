package com.soywiz.korio

import com.soywiz.korio.vfs.MapLikeStorageVfs
import com.soywiz.korio.vfs.SimpleStorage
import org.w3c.dom.get
import org.w3c.dom.set
import kotlin.browser.localStorage

val jsLocalStorageVfs by lazy {
	MapLikeStorageVfs(object : SimpleStorage {
		override suspend fun get(key: String): String? = localStorage[key]
		override suspend fun set(key: String, value: String) = run { localStorage[key] = value }
		override suspend fun remove(key: String) = localStorage.removeItem(key)
	})
}
