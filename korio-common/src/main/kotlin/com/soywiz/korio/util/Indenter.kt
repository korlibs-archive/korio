package com.soywiz.korio.util


object INDENTS {
	private val INDENTS = arrayListOf<String>("")

	operator fun get(index: Int): String {
		if (index >= INDENTS.size) {
			val calculate = INDENTS.size * 10
			var indent = INDENTS[INDENTS.size - 1]
			while (calculate >= INDENTS.size) {
				indent += "\t"
				INDENTS.add(indent)
			}
		}
		return if (index <= 0) "" else INDENTS[index]
	}
}

class Indenter(private val actions: ArrayList<Action> = arrayListOf<Indenter.Action>()) {
	interface Action {
		interface Text : Action {
			val str: String
		}

		data class Marker(val data: Any) : Action
		data class Inline(override val str: String) : Text
		data class Line(override val str: String) : Text
		data class LineDeferred(val callback: () -> Indenter) : Action
		object Indent : Action
		object Unindent : Action
	}

	val noIndentEmptyLines = true

	companion object {
		fun genString(init: Indenter.() -> Unit) = gen(init).toString()

		val EMPTY = Indenter.gen { }

		inline fun gen(init: Indenter.() -> Unit): Indenter {
			val indenter = Indenter()
			indenter.init()
			return indenter
		}

		fun single(str: String): Indenter = Indenter(arrayListOf(Action.Line(str)))

		operator fun invoke(str: String): Indenter = single(str)

		fun replaceString(templateString: String, replacements: Map<String, String>): String {
			val pattern = Regex("\\$(\\w+)")
			return pattern.replace(templateString) { result ->
				replacements[result.groupValues[1]] ?: ""
			}
		}
	}

	var out: String = ""

	fun inline(str: String) = this.apply { this.actions.add(Action.Inline(str)) }
	fun line(indenter: Indenter) = this.apply { this.actions.addAll(indenter.actions) }
	fun line(str: String) = this.apply { this.actions.add(Action.Line(str)) }
	fun line(str: String?) {
		if (str != null) line(str)
	}

	fun mark(data: Any) = this.apply { this.actions.add(Action.Marker(data)) }

	fun linedeferred(init: Indenter.() -> Unit): Indenter {
		this.actions.add(Action.LineDeferred({
			val indenter = Indenter()
			indenter.init()
			indenter
		}))
		return this
	}

	inline fun line(str: String, callback: () -> Unit): Indenter {
		line(if (str.isEmpty()) "{" else "$str {")
		indent(callback)
		line("}")
		return this
	}

	inline fun line(str: String, after: String = "", after2: String = "", callback: () -> Unit): Indenter {
		line(if (str.isEmpty()) "{ $after" else "$str { $after")
		indent(callback)
		line("}$after2")
		return this
	}

	inline fun indent(callback: () -> Unit): Indenter {
		_indent()
		try {
			callback()
		} finally {
			_unindent()
		}
		return this
	}

	fun _indent() {
		actions.add(Action.Indent)
	}

	fun _unindent() {
		actions.add(Action.Unindent)
	}

	fun toString(markHandler: ((sb: StringBuilder, line: Int, data: Any) -> Unit)?, doIndent: Boolean): String {
		val out = StringBuilder()
		var line = 0

		var newLine = true
		var indentIndex = 0

		fun eval(actions: List<Action>) {
			for (action in actions) {
				when (action) {
					is Action.Text -> {
						if (newLine) {
							if (noIndentEmptyLines && action.str.isEmpty()) {
								if (doIndent) out.append('\n')
								line++
							} else {
								if (doIndent) out.append(INDENTS[indentIndex]) else out.append(" ")
							}
						}
						out.append(action.str)
						if (action is Action.Line) {
							line += action.str.count { it == '\n' }
							if (doIndent) out.append('\n')
							line++
							newLine = true
						} else {
							newLine = false
						}
					}
					is Action.LineDeferred -> eval(action.callback().actions)
					Action.Indent -> indentIndex++
					Action.Unindent -> indentIndex--
					is Action.Marker -> {
						markHandler?.invoke(out, line, action.data)
					}
				}
			}
		}

		eval(actions)

		return out.toString()
	}

	fun toString(markHandler: ((sb: StringBuilder, line: Int, data: Any) -> Unit)?): String = toString(markHandler = markHandler, doIndent = true)
	fun toString(doIndent: Boolean = true, indentChunk: String = "\t"): String {
		val out = toString(markHandler = null, doIndent = doIndent)
		return if (indentChunk == "\t") out else out.replace("\t", indentChunk)
	}

	override fun toString(): String = toString(null, doIndent = true)
}
