package com.soywiz.vitaorganizer

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object VitaOrganizerSettings {
    var initialized = false
    val properties = Properties()
    val file = File("vitaorganizer/settings.properties")

    var vpkFolder: String
        set(v: String) {
            properties.setProperty("vpkFolder", v)
            writeProperties()
        }
        get() = ensureProperties().properties.getProperty("vpkFolder") ?: "."

    fun ensureProperties(): VitaOrganizerSettings {
        if (!initialized) {
            readProperties()
        }
        return this
    }

    fun readProperties() {
        if (file.exists()) {
            properties.load(ByteArrayInputStream(file.readBytes()))
        }
        initialized = true
    }

    fun writeProperties() {
        file.parentFile.mkdirs()
        val out = FileOutputStream(file)
        properties.store(out, "")
        out.close()
    }
}