package com.soywiz.korio.util

import java.math.BigDecimal
import java.math.BigInteger

fun String.toBigInt() = BigInteger(this)
fun String.toBigDecimal() = BigDecimal(this)
fun Int.toBigInt() = BigInteger.valueOf(this.toLong())
fun Long.toBigInt() = BigInteger.valueOf(this)
fun BigInteger.toBigDecimal() = BigDecimal(this)
fun Int.toBigDecimal() = BigDecimal(this)
fun Long.toBigDecimal() = BigDecimal(this)
fun Double.toBigDecimal() = BigDecimal(this)
