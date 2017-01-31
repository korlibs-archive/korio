package com.soywiz.korio.serialization.json

import com.soywiz.korio.util.*
import org.intellij.lang.annotations.Language
import java.io.IOException

object Json {
	fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IOException(msg)

	fun decode(@Language("json") s: String): Any? = StrReader(s).decode()

	inline fun <reified T : Any> decodeToType(@Language("json") s: String): T = decodeToType(s, T::class.java)
	@Suppress("UNCHECKED_CAST")
	fun <T> decodeToType(@Language("json") s: String, clazz: Class<T>): T = ClassFactory(clazz).create(decode(s) as Map<String, Any?>)

	fun StrReader.decode(): Any? {
		val ic = skipSpaces().read()
		when (ic) {
			'{' -> {
				val out = LinkedHashMap<String, Any?>()
				obj@ while (true) {
					when (skipSpaces().read()) {
						'}' -> break@obj; ',' -> continue@obj; else -> unread()
					}
					val key = decode() as String
					skipSpaces().expect(':')
					val value = decode()
					out[key] = value
				}
				return out
			}
			'[' -> {
				val out = arrayListOf<Any?>()
				array@ while (true) {
					when (skipSpaces().read()) {
						']' -> break@array; ',' -> continue@array; else -> unread()
					}
					val item = decode()
					out += item
				}
				return out
			}
			'-', '+', in '0'..'9' -> {
				unread()
				val res = readWhile { (it in '0'..'9') || it == '.' || it == 'e' || it == 'E' || it == '-' || it == '+' }
				return res.toNumber()
			}
			't', 'f', 'n' -> {
				unread()
				if (tryRead("true")) return true
				if (tryRead("false")) return false
				if (tryRead("null")) return null
				invalidJson()
			}
			'"' -> {
				unread()
				return readStringLit()
			}
			else -> invalidJson("Not expected '$ic'")
		}
	}

	@Language("json")
	fun encode(obj: Any?) = StringBuilder().apply { encode(obj, this) }.toString()

	fun encode(obj: Any?, b: StringBuilder) {
		when (obj) {
			null -> b.append("null")
			is Boolean -> b.append(if (obj) "true" else "false")
			is Map<*, *> -> {
				b.append('{')
				for ((i, v) in obj.entries.withIndex()) {
					if (i != 0) b.append(',')
					encode(v.key, b)
					b.append(':')
					encode(v.value, b)
				}
				b.append('}')
			}
			is Iterable<*> -> {
				b.append('[')
				for ((i, v) in obj.withIndex()) {
					if (i != 0) b.append(',')
					encode(v, b)
				}
				b.append(']')
			}
			is String -> encodeString(obj, b)
			is Number -> b.append("$obj")
			else -> encode(ClassFactory(obj.javaClass).toMap(obj), b)
		}
	}

	fun encodePretty(obj: Any?, indentChunk: String = "\t"): String = Indenter().apply {
		encodePretty(obj, this)
	}.toString(doIndent = true, indentChunk = indentChunk)

	fun encodePretty(obj: Any?, b: Indenter) {
		when (obj) {
			null -> b.inline("null")
			is Boolean -> b.inline(if (obj) "true" else "false")
			is Map<*, *> -> {
				b.line("{")
				b.indent {
					val entries = obj.entries
					for ((i, v) in entries.withIndex()) {
						if (i != 0) b.line(",")
						b.inline(encodeString("" + v.key))
						b.inline(": ")
						encodePretty(v.value, b)
						if (i == entries.size - 1) b.line("")
					}
				}
				b.inline("}")
			}
			is Iterable<*> -> {
				b.line("[")
				b.indent {
					val entries = obj.toList()
					for ((i, v) in entries.withIndex()) {
						if (i != 0) b.line(",")
						encodePretty(v, b)
						if (i == entries.size - 1) b.line("")
					}
				}
				b.inline("]")
			}
			is String -> b.inline(encodeString(obj))
			is Number -> b.inline("$obj")
			else -> encodePretty(ClassFactory(obj.javaClass).toMap(obj), b)
		}
	}

	private fun encodeString(str: String) = StringBuilder().apply { encodeString(str, this) }.toString()

	private fun encodeString(str: String, b: StringBuilder) {
		b.append('"')
		for (c in str) {
			when (c) {
				'\\' -> b.append("\\\\"); '/' -> b.append("\\/"); '\'' -> b.append("\\'")
				'"' -> b.append("\\\""); '\b' -> b.append("\\b"); '\u000c' -> b.append("\\f")
				'\n' -> b.append("\\n"); '\r' -> b.append("\\r"); '\t' -> b.append("\\t")
				else -> b.append(c)
			}
		}
		b.append('"')
	}
}