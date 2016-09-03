package util

@Suppress("NOTHING_TO_INLINE")
class UByteArray(val data: ByteArray) {
	constructor(size: Int) : this(ByteArray(size))
	val size: Int = data.size
	inline operator fun get(n: Int) = this.data[n].toInt() and 0xFF
	inline operator fun set(n: Int, v: Int) = let { this.data[n] = v.toByte() }
}

val Int.byte: Byte get() = this.toByte()
val Short.byte: Byte get() = this.toByte()
val Char.byte: Byte get() = this.toByte()

val Int.ubyte: Int get() = this.toInt() and 0xFF
val Short.ubyte: Int get() = this.toInt() and 0xFF
val Char.ubyte: Int get() = this.toInt() and 0xFF
val Byte.ubyte: Int get() = this.toInt() and 0xFF