package util

@Suppress("NOTHING_TO_INLINE")
class UShortArray(val data: ShortArray) {
	constructor(size: Int) : this(ShortArray(size))

	val size: Int = data.size
	inline operator fun get(n: Int) = this.data[n].toInt() and 0xFFFF
	inline operator fun set(n: Int, v: Int) = let { this.data[n] = v.toShort() }
}