package com.soywiz.korio.serialization.yaml

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.ListReader
import com.soywiz.korio.util.StrReader
import java.util.*

object Yaml {
	fun read(str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)

	private fun parseStr(str: String) = str.toIntOrNull() ?: str.toDoubleOrNull() ?: str

	//const val TRACE = true
	const val TRACE = false

	private fun read(s: ListReader<Token>, level: Int): Any? = s.run {
		var list: ArrayList<Any?>? = null
		var map: HashMap<String, Any?>? = null

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
				val item = s.peek()
				if (item.str == "-") {
					if (s.read().str != "-") invalidOp
					if (list == null) list = arrayListOf()
					if (TRACE) println("${levelStr}LIST_ITEM...")
					val res = read(s, level + 1)
					if (TRACE) println("${levelStr}LIST_ITEM: $res")
					list.add(res)
				} else {
					val key = s.read().str
					if (s.eof || s.peek().str != ":") {
						if (TRACE) println("${levelStr}LIT: $key")
						return parseStr(key)
					} else {
						if (map == null) map = LinkedHashMap()
						if (s.read().str != ":") invalidOp
						if (TRACE) println("${levelStr}MAP[$key]...")
						val value = read(s, level + 1)
						map[key] = value
						if (TRACE) println("${levelStr}MAP[$key]: $value")
					}
				}
			}
		}

		if (TRACE) println("${levelStr}RETURN: list=$list, map=$map")

		return list ?: map ?: "<eof>"
	}

	fun StrReader.tokenize(): List<Token> {
		val out = arrayListOf<Token>()

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
			out += Token.LINE(indentStr, indentLevel)
			while (hasMore) {
				val c = read()
				when (c) {
					':', '-', '[', ']' -> {
						flush(); out += Token.SYMBOL("$c")
					}
					'#' -> {
						flush(); readUntilLineEnd(); skip(); continue@linestart
					} // Ignore comments
					'\n' -> {
						flush(); continue@linestart
					}
					'"' -> {
						flush()
						// @TODO: Do Handle escaping
						val ss = readUntil { it == '"' }
						out += Token.ID(ss)
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
		data class SYMBOL(override val str: String) : Token
	}

	fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }
}