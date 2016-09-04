package com.soywiz.vitaorganizer

import java.io.InputStream
import java.util.zip.ZipFile

fun ZipFile.getInputStream(name: String): InputStream {
    return this.getInputStream(this.getEntry(name))
}

fun ZipFile.getBytes(name: String): ByteArray {
    return this.getInputStream(name).use {
        it.readBytes()
    }
}