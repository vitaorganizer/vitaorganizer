package com.soywiz.util

import java.io.File

operator fun File.get(child: String) = File(this, child)
operator fun File.set(child: String, contents: ByteArray) {
    this[child].writeBytes(contents)
}
