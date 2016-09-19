package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.getResourceString

object VitaOrganizerVersion {
	val currentVersion: String get() = getResourceString("com/soywiz/vitaorganizer/currentVersion.txt") ?: "unknown"
}
