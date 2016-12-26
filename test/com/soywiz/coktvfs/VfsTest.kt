package com.soywiz.coktvfs

import org.junit.Assert
import org.junit.Test

class VfsTest {
    @Test
    fun testZipStat() = sync {
        val helloZip = ResourcesVfs()["hello.zip"].openAsZip()
        Assert.assertEquals(
                "VfsStat(file=VfsFile(ResourcesVfs, hello.zip/hello/world.txt), exists=true, isDirectory=false, size=12)",
                helloZip["hello/world.txt"].stat().toString()
        )
    }
}