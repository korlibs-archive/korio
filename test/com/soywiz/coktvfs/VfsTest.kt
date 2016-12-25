package com.soywiz.coktvfs

import org.junit.Assert
import org.junit.Test
import java.io.File

class VfsTest {
    @Test
    fun name() {
        val list = sync {
            //println(UrlVfs(URL("http://google.es/")).readString())

            val root = LocalVfs(File("."))
            println(root["build.gradle"].stat())
            LocalVfs(File(".")).list().toList()
        }
        Assert.assertEquals(
                "",
                list.map { "${it.file}:${it.size}" }
        )
    }
}