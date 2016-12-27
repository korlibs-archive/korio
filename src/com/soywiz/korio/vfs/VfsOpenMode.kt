package com.soywiz.korio.vfs

enum class VfsOpenMode {
    READ,
    WRITE,
    APPEND,
    TRUNCATE_EXISTING,
    CREATE,
    CREATE_NEW,
}