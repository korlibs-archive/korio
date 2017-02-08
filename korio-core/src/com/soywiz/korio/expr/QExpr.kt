package com.soywiz.korio.expr

abstract class QExpr {
	data class EQ(val key: String, val value: Any?) : QExpr()
	data class AND(val l: QExpr, val r: QExpr) : QExpr()

	companion object {
		operator fun invoke(callback: Builder.() -> QExpr) = callback(Builder())
	}

	class Builder {
		infix fun String.eq(v: Any?) = QExpr.EQ(this, v)
		infix fun QExpr.and(v: QExpr) = QExpr.AND(this, v)
	}
}