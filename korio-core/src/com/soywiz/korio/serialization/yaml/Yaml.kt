package com.soywiz.korio.serialization.yaml

import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.util.ListReader
import com.soywiz.korio.util.StrReader

// @TODO: Move to korio
object Yaml {
	fun read(str: String) = read(ListReader(StrReader(str).tokenize()), level = 0)

	private fun parseStr(str: String) = str.toIntOrNull() ?: str.toDoubleOrNull() ?: str

	private fun readExpr(s: ListReader<Token>): Any? {
		var out = ""
		while (!s.eof && s.peek() !is Token.LINE) {
			val c = s.read()
			out += c.str
		}
		return parseStr(out.trim())
	}

	private fun read(s: ListReader<Token>, level: Int): Any? = s.run {
		var list: ArrayList<Any?>? = null
		var map: HashMap<String, Any?>? = null

		linehandle@ while (s.hasMore) {
			val token = s.peek()
			val line = token as Token.LINE
			if (level > line.level) {
				// child level
				val res = read(s, line.level)
				if (list != null) {
					list.add(res)
				}
			} else if (level < line.level) {
				// parent level
				break
			} else {
				// current level
				s.read()
				val item = s.peek()
				if (item is Token.SYMBOL && item.str == "-") {
					if (s.read().str != "-") invalidOp
					if (list == null) list = arrayListOf()
					list.add(readExpr(s))
				} else {
					if (map == null) map = LinkedHashMap()
					val key = s.read().str
					if (s.read().str != ":") invalidOp
					val value = readExpr(s)
					map[key.trim()] = value
				}
			}
		}

		list ?: map
	}

	fun StrReader.tokenize(): List<Token> {
		val out = arrayListOf<Token>()

		var str = ""
		fun flush() {
			if (str.isNotEmpty()) {
				out += Token.ID(str)
				str = ""
			}
		}

		linestart@ while (hasMore) {
			// Line start
			flush()
			val indent = readWhile { it.isWhitespace() }
			out += Token.LINE(indent)
			while (hasMore) {
				val c = read()
				when (c) {
					':', '-', '[', ']' -> {
						flush(); out += Token.SYMBOL("$c")
					}
				// Ignore comments
					'#' -> {
						flush()
						readUntilLineEnd()
						skip()
						continue@linestart
					}
					'\n' -> {
						flush()
						continue@linestart
					}
					'"' -> {
						flush()
						// @TODO: Do Handle escaping
						val ss = readUntil { it == '"' }
						out += Token.ID(ss)
					}
					else -> {
						str += c
					}
				}
			}
		}
		flush()
		return out
	}

	interface Token {
		val str: String
		data class LINE(override val str: String) : Token {
			val level = str.sumBy { if (it == '\t') 4 else 1 }
		}
		data class ID(override val str: String) : Token
		data class SYMBOL(override val str: String) : Token
	}
}

fun StrReader.getIndentation(): Int {
	var n = this.pos - 1
	var m = 0
	while (n > 0 && this.str[n] != '\n') {
		val c = this.str[n]
		if (!c.isWhitespace()) break
		if (c == '\t') m += 4 else m++
		n--
	}
	return m
}

fun StrReader.readUntilLineEnd() = this.readUntil { it == '\n' }