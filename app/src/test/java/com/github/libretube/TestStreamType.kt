package com.github.libretube

import org.junit.Test
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class TestStreamType {
    @Test
    fun printStreamTypes() {
        println("STREAMINFOITEM METHODS:")
        StreamInfoItem::class.java.methods.forEach {
            println(it.name)
        }
    }
}
