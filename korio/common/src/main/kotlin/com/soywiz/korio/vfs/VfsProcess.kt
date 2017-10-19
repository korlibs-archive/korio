package com.soywiz.korio.vfs

open class VfsProcessHandler {
	suspend open fun onOut(data: ByteArray): Unit {
	}

	suspend open fun onErr(data: ByteArray): Unit {
	}
}

class VfsProcessException(message: String) : com.soywiz.korio.IOException(message)