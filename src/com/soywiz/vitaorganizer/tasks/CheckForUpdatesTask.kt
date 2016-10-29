package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VitaOrganizerVersion
import com.soywiz.vitaorganizer.ext.openWebpage
import java.net.URL

class CheckForUpdatesTask(vitaOrganizer: VitaOrganizer, val showCurrentVersionDialog: Boolean = true) : VitaTask(vitaOrganizer) {
	override fun perform() {
		try {
			val text = URL("https://raw.githubusercontent.com/soywiz/vitaorganizer/master/lastVersion.txt").readText()
			if(text.isEmpty())
				throw Exception("No content received from update URL")

			val parts = text.lines()
			if(parts.count() < 2)
				throw Exception("Unexpected content")

			val lastVersion = parts[0]
			val lastVersionUrl = parts[1]

			val regexVersion =Regex("([0-9]{1,2})\\.([0-9]{1,2})\\.([0-9]{1,2})")
			val matchLastVersion = regexVersion.matchEntire(lastVersion)
			val matchCurrentVersion = regexVersion.matchEntire( VitaOrganizerVersion.currentVersion)

			if(matchLastVersion == null || matchCurrentVersion == null) {
				println("not matched")
				throw Exception("Invalid version strings");
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

			val lastVersionGroupValues = matchLastVersion.groupValues;
			val lastSoftwareVersion = SoftwareVersion(
				lastVersionGroupValues[1].toByte(),
				lastVersionGroupValues[2].toByte(),
				lastVersionGroupValues[3].toByte()
			)

			val currentVersionGroupValues = matchCurrentVersion.groupValues;
			val currentSoftwareVersion = SoftwareVersion(
				currentVersionGroupValues[1].toByte(),
				currentVersionGroupValues[2].toByte(),
				currentVersionGroupValues[3].toByte()
			)

			lastSoftwareVersion.print("Last")
			currentSoftwareVersion.print("Current")

			if(lastSoftwareVersion.equals(currentSoftwareVersion)) {
				if(showCurrentVersionDialog) {
					info(
						Texts.format("YOU_HAVE_LASTEST_VERSION", "version" to  currentSoftwareVersion.toString()),
						Texts.format("ACTIONS_TITLE")
					)
				}
				else {
					status(Texts.format("YOU_HAVE_LASTEST_VERSION", "version" to currentSoftwareVersion.toString()))
				}
			} else if(lastSoftwareVersion.didIncrement(currentSoftwareVersion)) {
				val result = warn(
					Texts.format("ACTIONS_TITLE"),
					Texts.format("NEW_VERSION_AVAILABLE", "lastVersion" to lastSoftwareVersion.toString(), "currentVersion" to currentSoftwareVersion.toString())
				)
				if (result)
					openWebpage(URL(lastVersionUrl))
			}
			else {
				println("decreased/changed?")
				throw Exception("Invalid")
			}
			println(parts)
		}
		catch(e: java.net.UnknownHostException) {
			error("${Texts.format("UPDATE_CHECK_FAILED")} ${Texts.format("UNKNOWN_REMOTE_HOST")} ${e.message!!}")
		}
		catch(e: Exception) {
			error("${Texts.format("UPDATE_CHECK_FAILED")} ${e.message!!}")
		}
		catch(e: Throwable) {
			error(Texts.format("UPDATE_CHECK_FAILED"))
			e.printStackTrace();
		}
	}
}