package com.soywiz.korio.serialization.xml

fun Iterable<Xml2>.str(name: String, defaultValue: String = ""): String = this.first().attributes[name] ?: defaultValue
fun Iterable<Xml2>.children(name: String): Iterable<Xml2> = this.flatMap { it.children(name) }
operator fun Iterable<Xml2>.get(name: String): Iterable<Xml2> = this.children(name)
fun String.toXml2(): Xml2 = Xml2.parse(this)
