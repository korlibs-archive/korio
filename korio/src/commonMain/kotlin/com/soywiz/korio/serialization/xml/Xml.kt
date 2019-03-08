package com.soywiz.korio.serialization.xml

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*

data class Xml(
	val type: Type,
	val name: String,
	val attributes: Map<String, String>,
	val allChildren: List<Xml>,
	val content: String
) {
	val attributesLC = attributes.toCaseInsensitiveMap()
	val nameLC: String = name.toLowerCase().trim()
	val descendants: Iterable<Xml> get() = allChildren.flatMap { it.descendants + it }
	val allChildrenNoComments get() = allChildren.filter { !it.isComment }
	val allNodeChildren get() = allChildren.filter { it.isNode }

	companion object {
		fun Tag(tagName: String, attributes: Map<String, Any?>, children: List<Xml>): Xml =
			Xml(Xml.Type.NODE, tagName, attributes.filter { it.value != null }.map { it.key to it.value.toString() }.toMap(), children, "")
		fun Text(text: String): Xml = Xml(Xml.Type.TEXT, "_text_", LinkedHashMap(), listOf(), text)
		fun Comment(text: String): Xml = Xml(Xml.Type.COMMENT, "_comment_", LinkedHashMap(), listOf(), text)

		//operator fun invoke(@Language("xml") str: String): Xml = parse(str)

		fun parse(str: String): Xml {
			try {
				val stream = Xml.Stream.parse(str).iterator()

				data class Level(val children: List<Xml>, val close: Xml.Stream.Element.CloseTag?)

				fun level(): Level {
					var children = listOf<Xml>()

					while (stream.hasNext()) {
						val tag = stream.next()
						when (tag) {
							is Xml.Stream.Element.ProcessingInstructionTag -> Unit
							is Xml.Stream.Element.CommentTag -> children += Xml.Comment(tag.text)
							is Xml.Stream.Element.Text -> children += Xml.Text(tag.text)
							is Xml.Stream.Element.OpenCloseTag -> children += Xml.Tag(tag.name, tag.attributes, listOf())
							is Xml.Stream.Element.OpenTag -> {
								val out = level()
								if (out.close?.name != tag.name) throw IllegalArgumentException("Expected ${tag.name} but was ${out.close?.name}")
								children += Xml(Xml.Type.NODE, tag.name, tag.attributes, out.children, "")
							}
							is Xml.Stream.Element.CloseTag -> return Level(children, tag)
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
						allChildren.fastForEach { child ->
							child.toOuterXmlIndented(indenter)
						}
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

	fun double(name: String, defaultValue: Double = 0.0): Double =
		this.attributesLC[name]?.toDoubleOrNull() ?: defaultValue

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

	object Entities {
		// Predefined entities in XML 1.0
		private val charToEntity = linkedMapOf('"' to "&quot;", '\'' to "&apos;", '<' to "&lt;", '>' to "&gt;", '&' to "&amp;")
		private val entities = StrReader.Literals.fromList(charToEntity.values.toTypedArray())
		private val entityToChar = charToEntity.flip()

		fun encode(str: String): String = str.eachBuilder {
			val entry = charToEntity[it]
			when {
				entry != null -> append(entry)
				else -> append(it)
			}
		}
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

	object Stream {
		fun parse(str: String): Iterable<Element> = parse(StrReader(str))
		fun parse(r: StrReader): Iterable<Element> = Xml2Iterable(r)

		private fun StrReader.matchStringOrId(): String? = matchSingleOrDoubleQuoteString() ?: matchIdentifier()

		private fun xmlSequence(r: StrReader) = sequence<Element> {
			while (!r.eof) {
				val str = r.readUntil('<') ?: ""
				if (str.isNotEmpty()) {
					yield(Element.Text(Xml.Entities.decode(str)))
				}

				if (r.eof) break

				r.skipExpect('<')
				when {
					r.tryExpect("![CDATA[") -> {
						val start = r.pos
						while (!r.eof) {
							val end = r.pos
							if (r.tryExpect("]]>")) {
								yield(Element.Text(r.createRange(start, end).text))
								break
							}
							r.readChar()
						}
					}
					r.tryExpect("!--") -> {
						val start = r.pos
						while (!r.eof) {
							val end = r.pos
							if (r.tryExpect("-->")) {
								yield(Element.CommentTag(r.createRange(start, end).text))
								break
							}
							r.readChar()
						}
					}
					else -> {
						r.skipSpaces()
						val processingInstruction = r.tryExpect('?')
						val processingEntityOrDocType = r.tryExpect('!')
						val close = r.tryExpect('/') || processingEntityOrDocType
						r.skipSpaces()
						val name = r.matchIdentifier()
							?: error("Couldn't match identifier after '<', offset=${r.pos}, around='${r.peek(10)}'")
						r.skipSpaces()
						val attributes = linkedMapOf<String, String>()
						while (r.peekChar() != '?' && r.peekChar() != '/' && r.peekChar() != '>') {
							val key = r.matchStringOrId() ?: throw IllegalArgumentException(
								"Malformed document or unsupported xml construct around ~${r.peek(10)}~ for name '$name'"
							)
							r.skipSpaces()
							if (r.tryExpect("=")) {
								r.skipSpaces()
								val argsQuote = r.matchStringOrId()
								attributes[key] = when {
									argsQuote != null -> Xml.Entities.decode(argsQuote.substring(1, argsQuote.length - 1))
									else -> Xml.Entities.decode(r.matchIdentifier()!!)
								}
							} else {
								attributes[key] = key
							}
							r.skipSpaces()
						}
						val openclose = r.tryExpect('/')
						val processingInstructionEnd = r.tryExpect('?')
						r.skipExpect('>')
						when {
							processingInstruction || processingEntityOrDocType ->
								yield(Element.ProcessingInstructionTag(name, attributes))
							openclose -> yield(Element.OpenCloseTag(name, attributes))
							close -> yield(Element.CloseTag(name))
							else -> yield(Element.OpenTag(name, attributes))
						}
					}
				}
			}
		}

		class Xml2Iterable(val reader2: StrReader) : Iterable<Element> {
			val reader = reader2.clone()
			override fun iterator(): Iterator<Element> = xmlSequence(reader).iterator()
		}

		sealed class Element {
			class ProcessingInstructionTag(val name: String, val attributes: Map<String, String>) : Element()
			class OpenCloseTag(val name: String, val attributes: Map<String, String>) : Element()
			class OpenTag(val name: String, val attributes: Map<String, String>) : Element()
			class CommentTag(val text: String) : Element()
			class CloseTag(val name: String) : Element()
			class Text(val text: String) : Element()
		}
	}
}

val Xml.isText get() = this.type == Xml.Type.TEXT
val Xml.isComment get() = this.type == Xml.Type.COMMENT
val Xml.isNode get() = this.type == Xml.Type.NODE

fun Iterable<Xml>.str(name: String, defaultValue: String = ""): String = this.first().attributes[name] ?: defaultValue
fun Iterable<Xml>.children(name: String): Iterable<Xml> = this.flatMap { it.children(name) }
val Iterable<Xml>.allChildren: Iterable<Xml> get() = this.flatMap(Xml::allChildren)
operator fun Iterable<Xml>.get(name: String): Iterable<Xml> = this.children(name)
fun String.toXml(): Xml = Xml.parse(this)

fun Xml(str: String): Xml = Xml.parse(str)

suspend fun VfsFile.readXml(): Xml = Xml(this.readString())
