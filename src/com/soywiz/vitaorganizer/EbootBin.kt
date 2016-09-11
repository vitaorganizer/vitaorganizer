package com.soywiz.vitaorganizer

import com.soywiz.util.Stream2

object EbootBin {
    fun hasExtendedPermissions(s: Stream2): Boolean {
        val s2 = s.slice()
        s2.position = 0x80
        val authid = s2.readS64_le()
        return when (authid) {
            0x2F00000000000001L, 0x2F00000000000003L -> true
            else -> false
        }
    }
}