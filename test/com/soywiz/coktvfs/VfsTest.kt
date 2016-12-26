package com.soywiz.coktvfs

import org.junit.Test

class VfsTest {
    @Test
    fun name() {
        val list = sync {
            val helloZip = ResourcesVfs()["hello.zip"].openAsZip()
            println(helloZip["hello/world.txt"].stat())
            //println(UrlVfs(URL("http://google.es/")).readString())

            //val root = LocalVfs(File("."))
            //println(root["build.gradle"].stat())
            //LocalVfs(File(".")).list().toList()
        }
        //Assert.assertEquals(
        //        "",
        //        list.map { "${it.file}:${it.size}" }
        //)
    }
}