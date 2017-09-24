package com.soywiz.korio.serialization.json

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.lang.IOException
import com.soywiz.korio.lang.KClass
import com.soywiz.korio.lang.Language
import com.soywiz.korio.serialization.ObjectMapper
import com.soywiz.korio.util.Indenter
import com.soywiz.korio.util.StrReader
import com.soywiz.korio.util.readStringLit
import com.soywiz.korio.util.toNumber
import kotlin.collections.set

object Json {
	fun stringifyPretty(obj: Any?, mapper: ObjectMapper) = stringify(obj, mapper, pretty = true)
	fun stringify(obj: Any?, mapper: ObjectMapper, pretty: Boolean = false) = if (pretty) encodePretty(obj, mapper) else encode(obj, mapper)
	fun parse(@Language("json") s: String): Any? = StrReader(s).decode()
	inline fun <reified T : Any> parseTyped(@Language("json") s: String, mapper: ObjectMapper): T = decodeToType(s, T::class, mapper)

	fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IOException(msg)

	fun decode(@Language("json") s: String): Any? = StrReader(s).decode()

	inline fun <reified T : Any> decodeToType(@Language("json") s: String, mapper: ObjectMapper): T = decodeToType(s, T::class, mapper)
	@Suppress("UNCHECKED_CAST")
	fun <T> decodeToType(@Language("json") s: String, clazz: KClass<T>, mapper: ObjectMapper): T = mapper.toTyped(decode(s), clazz)

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
	fun encode(obj: Any?, mapper: ObjectMapper) = StringBuilder().apply { encode(obj, this, mapper) }.toString()

	fun encode(obj: Any?, b: StringBuilder, mapper: ObjectMapper) {
		when (obj) {
			null -> b.append("null")
			is Boolean -> b.append(if (obj) "true" else "false")
			is Map<*, *> -> {
				b.append('{')
				for ((i, v) in obj.entries.withIndex()) {
					if (i != 0) b.append(',')
					encode(v.key, b, mapper)
					b.append(':')
					encode(v.value, b, mapper)
				}
				b.append('}')
			}
			is Iterable<*> -> {
				b.append('[')
				for ((i, v) in obj.withIndex()) {
					if (i != 0) b.append(',')
					encode(v, b, mapper)
				}
				b.append(']')
			}
			is Enum<*> -> encodeString(obj.name, b)
			is String -> encodeString(obj, b)
			is Number -> b.append("$obj")
			is CustomJsonSerializer -> obj.encodeToJson(b)
			else -> {
				invalidOp("Don't know how to serialize $obj")
				//encode(ClassFactory(obj::class).toMap(obj), b)
			}
		}
	}

	fun encodePretty(obj: Any?, mapper: ObjectMapper, indentChunk: String = "\t"): String = Indenter().apply {
		encodePretty(obj, mapper, this)
	}.toString(doIndent = true, indentChunk = indentChunk)

	fun encodePretty(obj: Any?, mapper: ObjectMapper, b: Indenter) {
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
						encodePretty(v.value, mapper, b)
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
						encodePretty(v, mapper, b)
						if (i == entries.size - 1) b.line("")
					}
				}
				b.inline("]")
			}
			is String -> b.inline(encodeString(obj))
			is Number -> b.inline("$obj")
		//else -> encodePretty(ClassFactory(obj::class).toMap(obj), b)
			is CustomJsonSerializer -> {
				b.inline(StringBuilder().apply { obj.encodeToJson(this) }.toString())
			}
			else -> {
				invalidOp("Don't know how to serialize $obj")
				//encode(ClassFactory(obj::class).toMap(obj), b)
			}
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

interface CustomJsonSerializer {
	fun encodeToJson(b: StringBuilder)
}