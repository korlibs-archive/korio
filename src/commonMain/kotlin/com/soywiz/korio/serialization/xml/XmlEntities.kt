package com.soywiz.korio.serialization.xml

import com.soywiz.kds.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

object XmlEntities {
	// Predefined entities in XML 1.0
	private val charToEntity = linkedMapOf('"' to "&quot;", '\'' to "&apos;", '<' to "&lt;", '>' to "&gt;", '&' to "&amp;")
	private val entities = StrReader.Literals.fromList(charToEntity.values.toTypedArray())
	private val entityToChar = charToEntity.flip()

	fun encode(str: String): String = str.transform { charToEntity[it] ?: "$it" }
	fun decode(str: String): String = decode(StrReader(str))
	fun decode(r: StrReader): String = buildString {
		while (!r.eof) {
			@Suppress("LiftReturnOrAssignment") // Performance?
			when (val c = r.readChar()) {
				'&' -> {
					val value = r.readUntilIncluded(';') ?: ""
					val full = "&$value"
					append(when {
						value.startsWith('#') -> "${value.substring(1, value.length - 1).toInt().toChar()}"
						entityToChar.contains(full) -> "${entityToChar[full]}"
						else -> full
					})
				}
				else -> append(c)
			}
		}
	}
}
