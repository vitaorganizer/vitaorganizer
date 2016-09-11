package com.soywiz.util

val Int.byte: Byte get() = this.toByte()
val Short.byte: Byte get() = this.toByte()
val Char.byte: Byte get() = this.toByte()

val Int.ubyte: Int get() = this.toInt() and 0xFF
val Short.ubyte: Int get() = this.toInt() and 0xFF
val Char.ubyte: Int get() = this.toInt() and 0xFF
val Byte.ubyte: Int get() = this.toInt() and 0xFF