package com.soywiz.vitaorganizer.ext

import java.util.*

fun Random.nextString(chars: String, len: Int): String {
	val out = CharArray(len)
	for (n in 0 until len) out[n] = chars[this.nextInt(chars.length)]
	return String(out)
}