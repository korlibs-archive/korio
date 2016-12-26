package com.soywiz.coktvfs

import com.soywiz.coktvfs.async.sync
import com.soywiz.coktvfs.async.toList
import org.junit.Assert
import org.junit.Test

class VfsTest {
    @Test
    fun testZipUncompressed() = sync {
        val helloZip = ResourcesVfs()["hello.zip"].openAsZip()

        Assert.assertEquals(
                "[VfsStat(file=ZipVfs(ResourcesVfs[hello.zip])[hello], exists=true, isDirectory=true, size=0)]",
                helloZip.list().toList().toString()
        )

        Assert.assertEquals(
                "[VfsStat(file=ZipVfs(ResourcesVfs[hello.zip])[hello/world.txt], exists=true, isDirectory=false, size=12)]",
                helloZip["hello"].list().toList().toString()
        )

        Assert.assertEquals(
                "VfsStat(file=ZipVfs(ResourcesVfs[hello.zip])[hello/world.txt], exists=true, isDirectory=false, size=12)",
                helloZip["hello/world.txt"].stat().toString()
        )
        Assert.assertEquals(
                "HELLO WORLD!",
                helloZip["hello/world.txt"].readString()
        )
    }

    @Test
    fun testZipCompressed() = sync {
        val helloZip = ResourcesVfs()["compressedHello.zip"].openAsZip()
        Assert.assertEquals(
                "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!",
                helloZip["hello/compressedWorld.txt"].readString()
        )
    }
}