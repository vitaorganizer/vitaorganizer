package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VitaOrganizerVersion
import com.soywiz.vitaorganizer.ext.openWebpage
import java.net.URL

class CheckForUpdatesTask(vitaOrganizer: VitaOrganizer, val showCurrentVersionDialog: Boolean = true) : VitaTask(vitaOrganizer) {
	override fun perform() {
		try {
			val lastSoftwareVersion = VitaOrganizerVersion.getLastVersion()
			val currentSoftwareVersion = VitaOrganizerVersion.getCurrentVersion()

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
					openWebpage(URL(VitaOrganizerVersion.lastVersionUrl))
			}
			else {
				println("decreased/changed?")
				throw Exception("Invalid")
			}
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
