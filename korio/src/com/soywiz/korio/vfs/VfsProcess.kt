package com.soywiz.korio.vfs

import java.io.IOException

open class VfsProcessHandler {
	suspend open fun onOut(data: ByteArray): Unit {
	}

	suspend open fun onErr(data: ByteArray): Unit {
	}
}

class VfsProcessException(message: String) : IOException(message)