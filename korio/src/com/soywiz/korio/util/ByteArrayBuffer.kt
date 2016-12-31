package com.soywiz.korio.util

import java.util.*

class ByteArrayBuffer(var data: ByteArray = ByteArray(0)) {
    var size: Int = 0
        get() = field
        set(len) {
            if (len > data.size.toLong()) {
                data = Arrays.copyOf(data, Math.max(len, (data.size + 7) * 2))
            }
            field = len
        }
}