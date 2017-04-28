package com.soywiz.korio.net

import com.soywiz.korio.service.Services

val asyncSocketFactory: AsyncSocketFactory by lazy {
	Services.load(AsyncSocketFactory::class.java)
}
