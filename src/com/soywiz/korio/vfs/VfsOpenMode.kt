package com.soywiz.korio.vfs

enum class VfsOpenMode(val str: String) {
    READ("r"),
    WRITE("w"),
    APPEND("a+"),
    TRUNCATE_EXISTING("t"),
    CREATE("c"),
    CREATE_NEW("cn");

    companion object {
        fun fromString(str: String): VfsOpenMode {
            if ('r' in str) {
                return READ
            }
            TODO()
        }
    }
}