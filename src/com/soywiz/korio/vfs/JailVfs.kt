package com.soywiz.korio.vfs

fun JailVfs(file: VfsFile): VfsFile {
    class Impl : Vfs.Proxy() {
        override fun access(path: String): VfsFile = file[VfsFile.normalize(path)]

        // @TODO: Implement this!
        override fun transformStat(stat: VfsStat): VfsStat {
            System.err.println("@TODO: Implement JailVfs.transformStat")
            return super.transformStat(stat)
        }
    }
    return Impl().root
}

