package com.soywiz.korio.serialization.json

import com.soywiz.korio.util.StrReader
import com.soywiz.korio.util.toNumber
import org.intellij.lang.annotations.Language
import java.io.IOException

object Json {
	fun invalidJson(msg: String = "Invalid JSON"): Nothing = throw IOException(msg)

	fun decode(@Language("json") s: String): Any? = StrReader(s).decode()

	fun StrReader.decode(): Any? {
		val ic = skipSpaces().read()
		when (ic) {
			'{' -> {
				val out = hashMapOf<String, Any?>()
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
				val out = StringBuilder()
				while (true) {
					val c = read()
					if (c == '\\') {
						val cc = read()
						out.append(when (cc) {
							'\\' -> '\\'; '/' -> '/'; '\'' -> '\''; '"' -> '"'
							'b' -> '\b'; 'f' -> '\u000c'; 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'
							else -> invalidJson()
						})
					} else if (c == '"') {
						break
					} else {
						out.append(c)
					}
				}
				return out.toString()
			}
			else -> invalidJson("Not expected '$ic'")
		}
	}

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
			is String -> {
				b.append('"')
				for (c in obj) {
					when (c) {
						'\\' -> b.append("\\\\")
						'/' -> b.append("\\/")
						'\'' -> b.append("\\'")
						'"' -> b.append("\\\"")
						'\b' -> b.append("\\b")
						'\u000c' -> b.append("\\f")
						'\n' -> b.append("\\n")
						'\r' -> b.append("\\r")
						'\t' -> b.append("\\t")
						else -> b.append(c)
					}
				}
				b.append('"')
			}
			is Number -> b.append("$obj")
			else -> invalidJson("Don't know how to handle $obj")
		}
	}
}