package com.soywiz.korio.expr

import kotlin.reflect.KProperty1

abstract class QExpr {
	data class BINOP(val op: String, val key: String, val value: Any?) : QExpr()
	data class AND(val l: QExpr, val r: QExpr) : QExpr()

	companion object {
		operator fun invoke(callback: Builder.() -> QExpr) = callback(Builder())
	}

	class Builder {
		infix fun String.eq(v: Any?) = QExpr.BINOP("=", this, v)
		infix fun String.gt(v: Any?) = QExpr.BINOP(">", this, v)
		infix fun String.ge(v: Any?) = QExpr.BINOP(">=", this, v)
		infix fun String.lt(v: Any?) = QExpr.BINOP("<", this, v)
		infix fun String.le(v: Any?) = QExpr.BINOP("<=", this, v)
		infix fun QExpr.and(v: QExpr) = QExpr.AND(this, v)
	}

	class TypedBuilder<T> {
		infix fun <T1> KProperty1<T, T1>.eq(v: T1) = QExpr.BINOP("=", this.name, v)
		infix fun <T1> KProperty1<T, T1>.gt(v: T1) = QExpr.BINOP(">", this.name, v)
		infix fun <T1> KProperty1<T, T1>.ge(v: T1) = QExpr.BINOP(">=", this.name, v)
		infix fun <T1> KProperty1<T, T1>.lt(v: T1) = QExpr.BINOP("<", this.name, v)
		infix fun <T1> KProperty1<T, T1>.le(v: T1) = QExpr.BINOP("<=", this.name, v)
		infix fun QExpr.and(v: QExpr) = QExpr.AND(this, v)
	}
}