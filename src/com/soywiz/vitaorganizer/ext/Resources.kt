package com.soywiz.vitaorganizer.ext

fun getResourceURL(name: String) = ClassLoader.getSystemResource(name)
fun getResourceBytes(name: String) = try {
	ClassLoader.getSystemResource(name).readBytes()
} catch (e: Throwable) {
	null
}

fun getResourceString(name: String) = try {
	ClassLoader.getSystemResource(name).readText()
} catch (e: Throwable) {
	null
}
