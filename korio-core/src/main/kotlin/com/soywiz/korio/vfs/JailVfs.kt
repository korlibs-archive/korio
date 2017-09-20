package com.soywiz.korio.vfs

fun JailVfs(jailRoot: VfsFile): VfsFile = object : Vfs.Proxy() {
	val baseJail = VfsUtil.normalize(jailRoot.path)

	override suspend fun access(path: String): VfsFile = jailRoot[VfsUtil.normalize(path).trim('/')]
	suspend override fun transform(out: VfsFile): VfsFile {
		val outPath = VfsUtil.normalize(out.path)
		if (!outPath.startsWith(baseJail)) throw UnsupportedOperationException("Jail not base root : ${out.path} | $baseJail")
		return file(outPath.substring(baseJail.length))
	}

	override val absolutePath: String get() = jailRoot.absolutePath

	override fun toString(): String = "JailVfs($jailRoot)"
}.root

