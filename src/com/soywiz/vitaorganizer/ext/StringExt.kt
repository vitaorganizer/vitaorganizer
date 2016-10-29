package com.soywiz.vitaorganizer.ext

fun String.parseInt(): Int = this.parseInt { 0 }

inline fun String.parseInt(default: (() -> Int)): Int = try {
	this.toInt()
} catch (e: Throwable) {
	default()
}

fun String.parseLong(): Long = this.parseLong { 0L }

inline fun String.parseLong(default: (() -> Long)): Long = try {
	this.toLong()
} catch (e: Throwable) {
	default()
}