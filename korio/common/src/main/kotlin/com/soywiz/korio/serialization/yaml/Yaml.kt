package com.soywiz.korio.serialization.yaml

import com.soywiz.korio.Language
import com.soywiz.korio.ds.LinkedList
import com.soywiz.korio.ds.lmapOf
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.serialization.ObjectMapper
import com.soywiz.korio.util.ListReader
import com.soywiz.korio.util.StrReader
import com.soywiz.korio.util.readStringLit
import com.soywiz.korio.util.unquote
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.MutableMap
import kotlin.collections.arrayListOf
import kotlin.collections.isNotEmpty
import kotlin.collections.last
import kotlin.collections.plusAssign
import kotlin.collections.set
import kotlin.reflect.KClass

object Yaml {
	fun decode(@Language("yaml") str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)
	inline fun <reified T : Any> decodeToType(@Language("yaml") s: String, mapper: ObjectMapper): T = decodeToType(s, T::class, mapper)
	@Suppress("UNCHECKED_CAST")
	fun <T : Any> decodeToType(@Language("yaml") s: String, clazz: KClass<T>, mapper: ObjectMapper): T = mapper.toTyped(clazz, decode(s))

	fun read(str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)

	private fun parseStr(tok: Token) = when (tok) {
		is Token.STR -> tok.ustr
		else -> parseStr(tok.str)
	}

	private fun parseStr(str: String) = when (str) {
		"null" -> null
		"true" -> true
		"false" -> false
		else -> str.toIntOrNull() ?: str.toDoubleOrNull() ?: str
	}

	//const val TRACE = true
	const val TRACE = false

	private fun read(s: ListReader<Token>, level: Int): Any? = s.run {
		var list: ArrayList<Any?>? = null
		var map: MutableMap<String, Any?>? = null

		val levelStr = if (TRACE) "  ".repeat(level) else ""

		linehandle@ while (s.hasMore) {
			val token = s.peek()
			val line = token as? Token.LINE
			val lineLevel = line?.level
			if (TRACE && line != null) println("${levelStr}LINE($lineLevel)")
			if (lineLevel != null && lineLevel > level) {
				// child level
				val res = read(s, lineLevel)
				if (list != null) {
					if (TRACE) println("${levelStr}CHILD.list.add: $res")
					list.add(res)
				} else {
					if (TRACE) println("${levelStr}CHILD.return: $res")
					return res
				}
			} else if (lineLevel != null && lineLevel < level) {
				// parent level
				if (TRACE) println("${levelStr}PARENT: level < line.level")
				break
			} else {
				// current level
				if (line != null) s.read()
				if (s.eof) break
				val item = s.peek()
				when (item.str) {
					"-" -> {
						if (s.read().str != "-") invalidOp
						if (list == null) list = arrayListOf()
						if (TRACE) println("${levelStr}LIST_ITEM...")
						val res = read(s, level + 1)
						if (TRACE) println("${levelStr}LIST_ITEM: $res")
						list.add(res)
					}
					"[" -> {
						if (s.read().str != "[") invalidOp
						val olist = arrayListOf<Any?>()
						array@ while (s.peek().str != "]") {
							olist += read(s, level + 1)
							val p = s.peek().str
							when (p) {
								"," -> {
									s.read(); continue@array
								}
								"]" -> break@array
								else -> invalidOp("Unexpected '$p'")
							}
						}
						if (s.read().str != "]") invalidOp
						return olist
					}
					else -> {
						val kkey = s.read()
						val key = kkey.str
						if (s.eof || s.peek().str != ":") {
							if (TRACE) println("${levelStr}LIT: $key")
							return parseStr(kkey)
						} else {
							if (map == null) map = lmapOf()
							if (s.read().str != ":") invalidOp
							if (TRACE) println("${levelStr}MAP[$key]...")
							val value = read(s, level + 1)
							map[key] = value
							if (TRACE) println("${levelStr}MAP[$key]: $value")
						}
					}
				}
			}
		}

		if (TRACE) println("${levelStr}RETURN: list=$list, map=$map")

		return list ?: map
	}

	fun StrReader.tokenize(): List<Token> {
		val out = arrayListOf<Token>()

		val s = this
		var str = ""
		fun flush() {
			if (str.isNotBlank() && str.isNotEmpty()) {
				out += Token.ID(str.trim()); str = ""
			}
		}

		val indents = LinkedList<Int>()
		linestart@ while (hasMore) {
			// Line start
			flush()
			val indentStr = readWhile(Char::isWhitespace).replace("\t", "     ")
			val indent = indentStr.length
			if (indents.isEmpty() || indent > indents.last) {
				indents += indent
			} else {
				while (indents.isNotEmpty() && indent < indents.last) indents.removeLast()
				if (indents.isEmpty()) invalidOp
			}
			val indentLevel = indents.size - 1
			while (out.isNotEmpty() && out.last() is Token.LINE) out.removeAt(out.size - 1)
			out += Token.LINE(indentStr, indentLevel)
			while (hasMore) {
				val c = read()
				when (c) {
					':', '-', '[', ']', ',' -> {
						flush(); out += Token.SYMBOL("$c")
					}
					'#' -> {
						flush(); readUntilLineEnd(); skip(); continue@linestart
					}
					'\n' -> {
						flush(); continue@linestart
					}
					'"', '\'' -> {
						flush()
						s.unread()
						out += Token.STR(s.readStringLit())
					}
					else -> str += c
				}
			}
		}
		flush()
		return out
	}

	interface Token {
		val str: String

		data class LINE(override val str: String, val level: Int) : Token {
			override fun toString(): String = "LINE($level)"
		}

		data class ID(override val str: String) : Token
		data class STR(override val str: String) : Token {
			val ustr = str.unquote()
		}

		data class SYMBOL(override val str: String) : Token
	}

	fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }
}