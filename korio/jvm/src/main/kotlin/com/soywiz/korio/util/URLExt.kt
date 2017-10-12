package com.soywiz.korio.util

import com.soywiz.korio.vfs.PathInfo
import java.net.URL

val URL.basename: String get() = PathInfo(this.file).basename