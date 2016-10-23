package com.soywiz.vitaorganizer.ext

import com.soywiz.util.open2

fun getResourceURL(name: String) = ClassLoader.getSystemResource(name)
fun getResourceBytes(name: String) = try {
	ClassLoader.getSystemResource(name).readBytes()
} catch (e: Throwable) {
	null
}

fun getResourceStream2(name: String) = getResourceBytes(name)?.open2("r")

fun getResourceString(name: String) = try {
	ClassLoader.getSystemResource(name).readText()
} catch (e: Throwable) {
	null
}
