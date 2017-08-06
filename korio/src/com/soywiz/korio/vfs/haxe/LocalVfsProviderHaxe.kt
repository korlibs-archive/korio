package com.soywiz.korio.vfs.haxe

import com.jtransc.JTranscSystem
import com.soywiz.korio.vfs.LocalVfs
import com.soywiz.korio.vfs.LocalVfsProvider

class LocalVfsProviderHaxe : LocalVfsProvider() {
	override val available: Boolean = JTranscSystem.isHaxe()

	override fun invoke(): LocalVfs {
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}