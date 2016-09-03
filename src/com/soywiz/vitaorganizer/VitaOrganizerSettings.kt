package com.soywiz.vitaorganizer

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.reflect.KProperty

object VitaOrganizerSettings {
    var vpkFolder: String by PropDelegate(".")
    var lastDeviceIp: String by PropDelegate("192.168.1.100")

    private var initialized = false
    private val properties = Properties()
    private val file = File("vitaorganizer/settings.properties")

    class PropDelegate(val default: String) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
            return VitaOrganizerSettings.ensureProperties().getProperty(property.name, default)
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            VitaOrganizerSettings.ensureProperties().setProperty(property.name, value)
            VitaOrganizerSettings.writeProperties()
        }
    }

    private fun ensureProperties(): Properties {
        if (!initialized) readProperties()
        return properties
    }

    private fun readProperties() {
        if (file.exists()) properties.load(ByteArrayInputStream(file.readBytes()))
        initialized = true
    }

    private fun writeProperties() {
        file.parentFile.mkdirs()
        val out = FileOutputStream(file)
        properties.store(out, "")
        out.close()
    }
}