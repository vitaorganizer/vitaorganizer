package com.soywiz.vitaorganizer

import java.io.*
import java.util.*
import kotlin.reflect.KProperty

object VitaOrganizerSettings {
	val CHARSET = Charsets.UTF_8
	var vpkFolder: String by PropDelegate(".")
	var lastDeviceIp: String by PropDelegate("192.168.1.100")
	var lastDevicePort: String by PropDelegate("1337")
	var LANGUAGE: String by PropDelegate("auto")

	val isLanguageAutodetect: Boolean get() = (LANGUAGE == "auto")
	val LANGUAGE_LOCALE: Locale get() = if (isLanguageAutodetect) Locale.getDefault() else Locale(LANGUAGE)

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
		if (file.exists()) properties.load(InputStreamReader(ByteArrayInputStream(file.readBytes()), CHARSET))
		initialized = true
	}

	private fun writeProperties() {
		file.parentFile.mkdirs()
		FileOutputStream(file).use { os ->
			OutputStreamWriter(os, CHARSET).use { writer ->
				properties.store(writer, "")
			}
		}
	}
}