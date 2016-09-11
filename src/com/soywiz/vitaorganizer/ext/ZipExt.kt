package com.soywiz.vitaorganizer.ext

import java.io.FileNotFoundException
import java.io.InputStream
import java.util.zip.ZipFile

fun ZipFile.getInputStream(name: String): InputStream {
    val entry = this.getEntry(name) ?: throw FileNotFoundException("Can't find '$name' inside zip")
    return this.getInputStream(entry)
}

fun ZipFile.getBytes(name: String): ByteArray {
    return this.getInputStream(name).use {
        it.readBytes()
    }
}