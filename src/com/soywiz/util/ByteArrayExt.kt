package com.soywiz.util

import java.util.*

fun ByteArray.getu(index: Int) = this[index].toInt() and 0xFF
fun ByteArray.setu(i: Int, v: Int) = run { this[i] = v.toByte() }
fun ByteArray.trimZero(): ByteArray {
    val index = this.indexOf(0)
    return if (index >= 0) {
        Arrays.copyOf(this, index)
    } else {
        this
    }
}

fun ByteArray.indexOf(smallerArray: ByteArray): Int {
    for (i in 0 until this.size - smallerArray.size + 1) {
        var found = true
        for (j in smallerArray.indices) {
            if (this[i + j] != smallerArray[j]) {
                found = false
                break
            }
        }
        if (found) return i
    }
    return -1
}

val HEX_DIGITS = "0123456789ABCDEF"
fun ByteArray.toHexString(): String {
    val out = CharArray(this.size * 2)
    var m = 0
    for (n in this.indices) {
        val v = this[n].toInt() and 0xFF
        out[m++] = HEX_DIGITS[(v ushr 4) and 0xF]
        out[m++] = HEX_DIGITS[(v ushr 0) and 0xF]
    }
    return String(out)
}
