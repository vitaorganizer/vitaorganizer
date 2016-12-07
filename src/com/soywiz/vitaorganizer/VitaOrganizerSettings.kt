package com.soywiz.vitaorganizer

import com.soywiz.util.HEX_DIGITS
import com.soywiz.util.OS
import com.soywiz.vitaorganizer.ext.nextString
import com.soywiz.vitaorganizer.ext.parseInt
import com.soywiz.vitaorganizer.ext.parseLong
import java.io.*
import java.security.SecureRandom
import java.util.*
import kotlin.reflect.KProperty

object VitaOrganizerSettings {
	private val queue by lazy { ThreadQueue() }
	private val CHARSET = Charsets.UTF_8
	var tableFontSize: Int by PropDelegateInt { 14 }
	var unitBase: Int by PropDelegateInt { if(OS.isWindows) 2 else 10 }
	var lastUpdateCheckTime: Long by PropDelegateLong { 0L }
	var WINDOW_WIDTH: Int by PropDelegateInt { 960 }
	var WINDOW_HEIGHT: Int by PropDelegateInt { 600 }
	var WINDOW_STATE: Int by PropDelegateInt { 0 }
	var vpkFolder: String by PropDelegateStr { "." }
	var lastVpkInstallFolder: String by PropDelegateStr{ "." }
	var lastDeviceIp: String by PropDelegateStr { "192.168.1.100" }
	var lastDevicePort: Int by PropDelegateInt { 1337 }
	var LANGUAGE: String by PropDelegateStr { "auto" }
	var WLAN_SSID: String by PropDelegateStr {
		"VORG-" + SecureRandom().nextString(HEX_DIGITS, 4)
	}
	var WLAN_PASS: String by PropDelegateStr {
		SecureRandom().nextString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789", 8)
	}

	val isLanguageAutodetect: Boolean get() = (LANGUAGE == "auto")
	val LANGUAGE_LOCALE: Locale get() = if (isLanguageAutodetect) Locale.getDefault() else Locale(LANGUAGE)

	private var initialized = false
	private val properties = Properties()
	internal val file = File("vitaorganizer/settings.properties")

	fun init() {
		ensureProperties()
		writeProperties()
	}

	fun ensureWriteSync() {
		writeProperties()
		queue.waitCompleted()
	}

	private class PropDelegateStr(val default: () -> String) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
			val props = VitaOrganizerSettings.ensureProperties()
			val key = property.name
			if (props.getProperty(key) == null) {
				props.setProperty(key, default())
				writeProperties()
			}
			return props.getProperty(key)
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
			VitaOrganizerSettings.ensureProperties().setProperty(property.name, value)
			VitaOrganizerSettings.writePropertiesAsync()
		}
	}

	private class PropDelegateInt(val default: () -> Int) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
			val props = VitaOrganizerSettings.ensureProperties()
			val key = property.name
			if (props.getProperty(key) == null) {
				props.setProperty(key, "" + default())
				writeProperties()
			}
			return props.getProperty(key).parseInt()
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
			VitaOrganizerSettings.ensureProperties().setProperty(property.name, "$value")
			VitaOrganizerSettings.writePropertiesAsync()
		}
	}

	private class PropDelegateLong(val default: () -> Long) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>): Long {
			val props = ensureProperties()
			val key = property.name
			if (props.getProperty(key) == null) {
				props.setProperty(key, "" + default())
				writeProperties()
			}
			return props.getProperty(key).parseLong()
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
			VitaOrganizerSettings.ensureProperties().setProperty(property.name, "$value")
			VitaOrganizerSettings.writePropertiesAsync()
		}
	}

	private fun ensureProperties(): Properties {
		if (!initialized) readProperties()
		return properties
	}

	private fun readProperties() {
		queue.waitCompleted()
		if (file.exists()) properties.load(InputStreamReader(ByteArrayInputStream(file.readBytes()), CHARSET))
		initialized = true
	}

	private fun writePropertiesAsync() {
		queue.clear()
		queue.queueAfter(100) {
			writeProperties()
		}
	}

	private fun writeProperties() {
		file.parentFile.mkdirs()
		FileOutputStream(file).use { os ->
			OutputStreamWriter(os, CHARSET).use { writer ->
				properties.store(writer, "")
			}
		}
		println("properties written!")
		//println(Thread.currentThread().stackTrace.toList().joinToString("\n"))
	}
}
