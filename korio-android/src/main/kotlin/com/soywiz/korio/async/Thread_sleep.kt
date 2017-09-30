package com.soywiz.korio.async

actual fun Thread_sleep(time: Long) = Thread.sleep(time)