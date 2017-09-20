package com.soywiz.korio.math

import java.lang.Math as JMath

impl object Math {
	impl fun min(a: Int, b: Int): Int = JMath.min(a, b)
	impl fun min(a: Long, b: Long): Long = JMath.min(a, b)
	impl fun max(a: Int, b: Int): Int = JMath.max(a, b)
	impl fun max(a: Long, b: Long): Long = JMath.max(a, b)
	impl fun pow(a: Double, b: Double): Double = JMath.pow(a, b)
	impl fun ceil(v: Double): Double = JMath.ceil(v)
	impl fun floor(v: Double): Double = JMath.floor(v)
	impl fun round(v: Double): Long = JMath.round(v)
}