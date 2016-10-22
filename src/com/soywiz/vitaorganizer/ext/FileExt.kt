package com.soywiz.vitaorganizer.ext

import java.io.File

fun File.listdirRecursively(): List<File> {
	val out = arrayListOf<File>()
	this.listdirRecursively { out += it }
	return out
}

fun File.listdirRecursively(emit: (file: File) -> Unit) {
	for (child in this.listFiles()) {
		emit(child)
		if (child.isDirectory) {
			child.listdirRecursively(emit)
		}
	}
}