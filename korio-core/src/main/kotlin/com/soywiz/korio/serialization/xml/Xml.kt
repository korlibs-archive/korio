package com.soywiz.korio.serialization.xml

import com.soywiz.korio.lang.Language
import com.soywiz.korio.util.Indenter
import com.soywiz.korio.util.toCaseInsensitiveTreeMap

data class Xml(val type: Type, val name: String, val attributes: Map<String, String>, val allChildren: List<Xml>, val content: String) {
	val attributesLC = attributes.toCaseInsensitiveTreeMap()
	val nameLC: String = name.toLowerCase().trim()
	val descendants: Iterable<Xml> get() = allChildren.flatMap { it.descendants + it }
	val allChildrenNoComments get() = allChildren.filter { !it.isComment }
	val allNodeChildren get() = allChildren.filter { it.isNode }

	companion object {
		fun Tag(tagName: String, attributes: Map<String, Any?>, children: List<Xml>): Xml {
			val att = attributes.filter { it.value != null }.map { it.key to it.value.toString() }.toMap()
			return Xml(Xml.Type.NODE, tagName, att, children, "")
		}

		fun Text(text: String): Xml {
			return Xml(Xml.Type.TEXT, "_text_", mapOf(), listOf(), text)
		}

		fun Comment(text: String): Xml {
			return Xml(Xml.Type.COMMENT, "_comment_", mapOf(), listOf(), text)
		}

		//operator fun invoke(@Language("xml") str: String): Xml = parse(str)

		fun parse(@Language("xml") str: String): Xml {
			try {
				val stream = XmlStream.parse(str).iterator()

				data class Level(val children: List<Xml>, val close: XmlStream.Element.CloseTag?)

				fun level(): Level {
					var children = listOf<Xml>()

					while (stream.hasNext()) {
						val tag = stream.next()
						when (tag) {
							is XmlStream.Element.ProcessingInstructionTag -> Unit
							is XmlStream.Element.CommentTag -> children += Xml.Comment(tag.text)
							is XmlStream.Element.Text -> children += Xml.Text(tag.text)
							is XmlStream.Element.OpenCloseTag -> children += Xml.Tag(tag.name, tag.attributes, listOf())
							is XmlStream.Element.OpenTag -> {
								val out = level()
								if (out.close?.name != tag.name) throw IllegalArgumentException("Expected ${tag.name} but was ${out.close?.name}")
								children += Xml(Xml.Type.NODE, tag.name, tag.attributes, out.children, "")
							}
							is XmlStream.Element.CloseTag -> return Level(children, tag)
							else -> throw IllegalArgumentException("Unhandled $tag")
						}
					}

					return Level(children, null)
				}

				val children = level().children
				return children.firstOrNull { it.type == Xml.Type.NODE }
					?: children.firstOrNull()
					?: Xml.Text("")
			} catch (t: NoSuchElementException) {
				println("ERROR: XML: $str thrown a NoSuchElementException")
				return Xml.Text("!!ERRORED!!")
			}
		}
	}

	val text: String
		get() = when (type) {
			Type.NODE -> allChildren.map { it.text }.joinToString("")
			Type.TEXT -> content
			Type.COMMENT -> ""
		}

	fun toOuterXmlIndented(indenter: Indenter = Indenter()): Indenter = indenter.apply {
		when (type) {
			Type.NODE -> {
				if (allChildren.isEmpty()) {
					line("<$name$attributesStr/>")
				} else {
					line("<$name$attributesStr>")
					indent {
						for (child in allChildren) child.toOuterXmlIndented(indenter)
					}
					line("</$name>")
				}
			}
			Type.TEXT -> line(content)
			Type.COMMENT -> line("<!--$content-->")
		}
	}

	val attributesStr: String get() = attributes.toList().map { " ${it.first}=\"${it.second}\"" }.joinToString("")

	val outerXml: String
		get() = when (type) {
			Type.NODE -> {
				if (allChildren.isEmpty()) {
					"<$name$attributesStr/>"
				} else {
					val children = this.allChildren.map(Xml::outerXml).joinToString("")
					"<$name$attributesStr>$children</$name>"
				}
			}
			Type.TEXT -> content
			Type.COMMENT -> "<!--$content-->"
		}

	val innerXml: String
		get() = when (type) {
			Type.NODE -> this.allChildren.map(Xml::outerXml).joinToString("")
			Type.TEXT -> content
			Type.COMMENT -> "<!--$content-->"
		}

	operator fun get(name: String): Iterable<Xml> = children(name)

	fun children(name: String): Iterable<Xml> = allChildren.filter { it.name.equals(name, ignoreCase = true) }
	fun child(name: String): Xml? = children(name).firstOrNull()
	fun childText(name: String): String? = child(name)?.text

	fun hasAttribute(key: String): Boolean = this.attributesLC.containsKey(key)
	fun attribute(name: String): String? = this.attributesLC[name]

	fun getString(name: String): String? = this.attributesLC[name]
	fun getInt(name: String): Int? = this.attributesLC[name]?.toInt()
	fun getLong(name: String): Long? = this.attributesLC[name]?.toLong()
	fun getDouble(name: String): Double? = this.attributesLC[name]?.toDouble()
	fun getFloat(name: String): Float? = this.attributesLC[name]?.toFloat()

	fun double(name: String, defaultValue: Double = 0.0): Double = this.attributesLC[name]?.toDoubleOrNull() ?: defaultValue
	fun float(name: String, defaultValue: Float = 0f): Float = this.attributesLC[name]?.toFloatOrNull() ?: defaultValue
	fun int(name: String, defaultValue: Int = 0): Int = this.attributesLC[name]?.toIntOrNull() ?: defaultValue
	fun long(name: String, defaultValue: Long = 0): Long = this.attributesLC[name]?.toLongOrNull() ?: defaultValue
	fun str(name: String, defaultValue: String = ""): String = this.attributesLC[name] ?: defaultValue

	fun doubleNull(name: String): Double? = this.attributesLC[name]?.toDoubleOrNull()
	fun floatNull(name: String): Float? = this.attributesLC[name]?.toFloatOrNull()
	fun intNull(name: String): Int? = this.attributesLC[name]?.toIntOrNull()
	fun longNull(name: String): Long? = this.attributesLC[name]?.toLongOrNull()
	fun strNull(name: String): String? = this.attributesLC[name]

	//override fun toString(): String = innerXml
	override fun toString(): String = outerXml

	enum class Type { NODE, TEXT, COMMENT }
}

val Xml.isText get() = this.type == Xml.Type.TEXT
val Xml.isComment get() = this.type == Xml.Type.COMMENT
val Xml.isNode get() = this.type == Xml.Type.NODE