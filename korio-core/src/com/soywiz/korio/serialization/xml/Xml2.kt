package com.soywiz.korio.serialization.xml

import org.intellij.lang.annotations.Language
import java.util.*

data class Xml2(val type: Type, val name: String, val attributes: Map<String, String>, val allChildren: List<Xml2>, val content: String) {
	val descendants: Iterable<Xml2> get() = allChildren.flatMap { it.allChildren + it }
	val allChildrenNoComments get() = allChildren.filter { it.type != Type.COMMENT }

	companion object {
		fun Tag(tagName: String, attributes: Map<String, Any?>, children: List<Xml2>): Xml2 {
			return Xml2(Xml2.Type.NODE, tagName, attributes.filter { it.value != null }.map { it.key to it.value.toString() }.toMap(), children, "")
		}

		fun Text(text: String): Xml2 {
			return Xml2(Xml2.Type.TEXT, "_text_", mapOf(), listOf(), text)
		}

		fun Comment(text: String): Xml2 {
			return Xml2(Xml2.Type.COMMENT, "_comment_", mapOf(), listOf(), text)
		}

		operator fun invoke(@Language("xml") str: String): Xml2 = parse(str)

		fun parse(@Language("xml") str: String): Xml2 {
			try {
				val stream = Xml2Stream.parse(str).iterator()

				data class Level(val children: List<Xml2>, val close: Xml2Stream.Element.CloseTag?)

				fun level(): Level {
					var children = listOf<Xml2>()

					while (stream.hasNext()) {
						val tag = stream.next()
						when (tag) {
							is Xml2Stream.Element.ProcessingInstructionTag -> Unit
							is Xml2Stream.Element.CommentTag -> children += Xml2.Comment(tag.text)
							is Xml2Stream.Element.Text -> children += Xml2.Text(tag.text)
							is Xml2Stream.Element.OpenCloseTag -> children += Xml2.Tag(tag.name, tag.attributes, listOf())
							is Xml2Stream.Element.OpenTag -> {
								val out = level()
								if (out.close?.name != tag.name) throw IllegalArgumentException("Expected ${tag.name} but was ${out.close?.name}")
								children += Xml2(Xml2.Type.NODE, tag.name, tag.attributes, out.children, "")
							}
							is Xml2Stream.Element.CloseTag -> return Level(children, tag)
							else -> throw IllegalArgumentException("Unhandled $tag")
						}
					}

					return Level(children, null)
				}

				val children = level().children
				return children.firstOrNull { it.type == Xml2.Type.NODE }
					?: children.firstOrNull()
					?: Xml2.Text("")
			} catch (t: NoSuchElementException) {
				println("ERROR: XML: $str thrown a NoSuchElementException")
				return Xml2.Text("!!ERRORED!!")
			}
		}
	}

	fun hasAttribute(key: String): Boolean = this.attributes.containsKey(key)
	fun attribute(name: String): String? = this.attributes[name]
	fun getString(name: String): String? = this.attributes[name]?.toString()
	fun getInt(name: String): Int? = this.attributes[name]?.toInt()
	fun getDouble(name: String): Double? = this.attributes[name]?.toDouble()

	val text: String get() = when (type) {
		Type.NODE -> allChildren.map { it.text }.joinToString("")
		Type.TEXT -> content
		Type.COMMENT -> ""
	}

	val outerXml: String get() = when (type) {
		Type.NODE -> {
			val attrs = attributes.toList().map { " ${it.first}=\"${it.second}\"" }.joinToString("")
			if (allChildren.isEmpty()) {
				"<$name$attrs/>"
			} else {
				val children = this.allChildren.map { it.outerXml }.joinToString("")
				"<$name$attrs>$children</$name>"
			}
		}
		Type.TEXT -> content
		Type.COMMENT -> "<!--$content-->"
	}

	val innerXml: String get() = when (type) {
		Type.NODE -> this.allChildren.map { it.outerXml }.joinToString("")
		Type.TEXT -> content
		Type.COMMENT -> "<!--$content-->"
	}

	operator fun get(name: String): Iterable<Xml2> = children(name)
	fun children(name: String): Iterable<Xml2> = allChildren.filter { it.name == name }
	fun child(name: String): Xml2? = children(name).firstOrNull()
	fun childText(name: String): String? = child(name)?.text

	fun double(name: String, defaultValue: Double = 0.0): Double = this.attributes[name]?.toDoubleOrNull() ?: defaultValue
	fun int(name: String, defaultValue: Int = 0): Int = this.attributes[name]?.toInt() ?: defaultValue
	fun str(name: String, defaultValue: String = ""): String = this.attributes[name] ?: defaultValue

	override fun toString(): String = innerXml

	enum class Type { NODE, TEXT, COMMENT }
}
