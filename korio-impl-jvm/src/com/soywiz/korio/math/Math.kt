package com.soywiz.korio.math

import java.lang.Math as JMath

impl object Math {
	impl fun min(a: Int, b: Int): Int = JMath.min(a, b)
}