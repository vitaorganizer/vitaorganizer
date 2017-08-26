package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.getResourceString
import java.net.URL

object VitaOrganizerVersion {
	val currentVersion: String get() = getResourceString("com/soywiz/vitaorganizer/currentVersion.txt") ?: "unknown"
	val lastVersion: String get() =  URL("https://raw.githubusercontent.com/vitaorganizer/vitaorganizer/master/lastVersion.txt").readText()

	var lastVersionUrl: String? = null

	fun getCurrentVersion() : SoftwareVersion = parseVersion(currentVersion)

	fun getLastVersion() : SoftwareVersion	{
		if(lastVersion.isEmpty())
			throw Exception("No content received from update URL")

		val parts = lastVersion.lines()
		if(parts.count() < 2)
			throw Exception("Unexpected content")

		lastVersionUrl = parts[1]
		return parseVersion(parts[0])
	}

	private fun parseVersion(versionString: String) : SoftwareVersion {
		val regexVersion =Regex("([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{1,2})")
		val matchVersion = regexVersion.matchEntire(versionString)

		if(matchVersion == null) {
			println("not matched")
			throw Exception("Invalid version string");
		}

		val versionGroupValues = matchVersion.groupValues;
		return SoftwareVersion(
			versionGroupValues[1].toByte(),
			versionGroupValues[2].toByte(),
			versionGroupValues[3].toByte())
	}
}

class SoftwareVersion(val major: Byte, val minor: Byte, val revision: Byte) {

	fun print(prefix: String) {
		println("[$prefix] Major($major) Minor($minor) Revision($revision)")
	}

	fun didIncrement(version: SoftwareVersion) : Boolean {
		if(version.major < this.major) return true;
		if(version.minor < this.minor) return true;
		if(version.revision < this.revision) return true;
		return false;
	}

	fun equals(version: SoftwareVersion) : Boolean {
		if(version.major == this.major
		   && version.minor == this.minor
		   && version.revision == this.revision)
			return true
		return false
	}

	override fun toString() : String = "${major}.${minor}.${revision}"
}

