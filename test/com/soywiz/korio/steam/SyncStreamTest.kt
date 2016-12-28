package com.soywiz.korio.steam

import com.soywiz.korio.stream.*
import org.junit.Assert
import org.junit.Test

class SyncStreamTest {
    @Test
    fun name() {
        val out = MemorySyncStream()
        Assert.assertEquals(0L, out.position)
        Assert.assertEquals(0L, out.length)
        out.write8(0x01)
        out.write8(0x02)
        out.write16_le(0x0304)
        Assert.assertEquals(4L, out.position)
        Assert.assertEquals(4L, out.length)
        out.position = 0L
        Assert.assertEquals(0L, out.position)
        Assert.assertEquals(4L, out.length)
        Assert.assertEquals(0x0102, out.readU16_be())
        Assert.assertEquals(0x0304, out.readU16_le())
        Assert.assertEquals(14, out.data.size)
        Assert.assertEquals(4, out.toByteArray().size)
        Assert.assertEquals(4, out.toByteArraySlice().length)
    }
}