package com.soywiz.korio.vfs

enum class VfsOpenMode(val str: String, val createIfNotExists: Boolean) {
    READ("r", createIfNotExists = false),
    WRITE("w", createIfNotExists = true),
    APPEND("a+", createIfNotExists = true),
    CREATE_OR_TRUNCATE("t+", createIfNotExists = false),
    CREATE("c", createIfNotExists = true),
    CREATE_NEW("cn", createIfNotExists = false);

    companion object {
        fun fromString(str: String): VfsOpenMode {
            if ('r' in str) {
                return READ
            }
            TODO()
        }
    }
}