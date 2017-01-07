package com.soywiz.korio.net

import java.util.*

val asyncSocketFactory: AsyncSocketFactory by lazy {
	ServiceLoader.load(AsyncSocketFactory::class.java).firstOrNull()
		?: throw UnsupportedOperationException("AsyncClientFactory implementation not found!")
}
