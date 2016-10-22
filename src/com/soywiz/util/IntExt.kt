package com.soywiz.util

val Int.byte: Byte get() = this.toByte()
val Short.byte: Byte get() = this.toByte()
val Char.byte: Byte get() = this.toByte()

val Int.ubyte: Int get() = this.toInt() and 0xFF
val Short.ubyte: Int get() = this.toInt() and 0xFF
val Char.ubyte: Int get() = this.toInt() and 0xFF
val Byte.ubyte: Int get() = this.toInt() and 0xFF

fun Int.toBitString(count: Int = 32): String {
	val out = CharArray(count)
	for (n in 0 until count) {
		out[count - n - 1] = if (((this ushr n) and 1) != 0) '1' else '0'
	}
	return String(out)
}