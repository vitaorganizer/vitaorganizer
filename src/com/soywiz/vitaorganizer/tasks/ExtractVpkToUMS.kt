package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import com.soywiz.vitaorganizer.ext.safe_delete
import com.soywiz.vitaorganizer.ext.safe_exists
import java.io.File

/**
 * Created by super on 14.01.2017.
 */

class ExtractVpkToUMS(vitaOrganizer: VitaOrganizer, val entry: CachedVpkEntry) :VitaTask(vitaOrganizer) {
	override fun perform() {
		if(!extract())
			error("Could not extract VPK to directory!")
		else {
			status("Successfully extracted. Use VitaShell to install.")
			info(
				"Successfully extracted ${entry.title} to ux0:organizer/${entry.gameId}\nNow select the directory in VitaShell and install it using the file options menu! This will only take seconds!")
		}
	}

	fun extract() : Boolean {
		val dir = File(VitaOrganizerSettings.usbMassStoragePath)
		val vpk = entry.vpkLocalFile!!

		//i dont knoe how this behaves on unix based systems. freeSpace only returns not 0 when it is a partition
		//so, will java recognize that mountpoint == partition (mounted from a partition)?
		//leave 50MB
		if((dir.freeSpace - 50*1000*1000) < entry.size) {
			error("Not enough available free space to extract the VPK file!")
			return false
		}

		val extractDir = dir.canonicalPath + File.separator + "organizer" + File.separator +  entry.gameId + File.separator
		val fileExtractDir = File(extractDir)
		if(fileExtractDir.safe_exists()) {
			val choice = warn("Overwrite?", "The directory where it is going to be extracted already exists.\nDo you want to overwrite it?")
			if(choice) {
				if(!fileExtractDir.safe_delete()) {
					error("Could not delete the directory (completely). Aborting..")
					return false
				}
            }
			else {
				return false
            }
		}

		fileExtractDir.mkdirs()

		status("Extracting ${entry.title} [${entry.gameId}]... Please wait...")

		if(!ZipMgr.extractZip(vpk, fileExtractDir))
			return false

		return true
	}
}
