package com.soywiz.korio.vfs

impl val tmpdir: String get() = System.getProperty("java.io.tmpdir")