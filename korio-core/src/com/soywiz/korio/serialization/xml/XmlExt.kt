package com.soywiz.korio.serialization.xml

import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.vfs.VfsFile
import org.intellij.lang.annotations.Language

fun Iterable<Xml>.str(name: String, defaultValue: String = ""): String = this.first().attributes[name] ?: defaultValue
fun Iterable<Xml>.children(name: String): Iterable<Xml> = this.flatMap { it.children(name) }
operator fun Iterable<Xml>.get(name: String): Iterable<Xml> = this.children(name)
fun String.toXml(): Xml = Xml.parse(this)

fun Xml(@Language("xml") str: String): Xml = Xml.parse(str)

suspend fun VfsFile.readXml(): Xml = asyncFun { Xml(this.readString()) }