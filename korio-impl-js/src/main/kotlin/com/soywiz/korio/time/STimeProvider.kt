package com.soywiz.korio.time

import kotlin.js.Date

impl fun currentTimeMillis() = Date().getTime().toLong()
