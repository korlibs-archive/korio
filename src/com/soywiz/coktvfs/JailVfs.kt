package com.soywiz.coktvfs

class JailVfs(val file: VfsFile) : Vfs.Proxy() {
    override fun access(path: String): VfsFile = file[VfsFile.normalize(path)]

    // @TODO: Implement this!
    override fun transformStat(stat: VfsStat): VfsStat {
        System.err.println("@TODO: Implement JailVfs.transformStat")
        return super.transformStat(stat)
    }
}