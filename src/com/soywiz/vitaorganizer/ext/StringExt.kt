package com.soywiz.vitaorganizer.ext

fun String.parseInt(): Int = this.parseInt { 0 }

inline fun String.parseInt(default: (() -> Int)): Int = try {
	this.toInt()
} catch (e: Throwable) {
	default()
}