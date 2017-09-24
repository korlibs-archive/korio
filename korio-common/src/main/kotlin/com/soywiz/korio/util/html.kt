package com.soywiz.korio.util

import com.soywiz.korio.serialization.xml.XmlEntities

fun String.htmlspecialchars() = XmlEntities.encode(this)
