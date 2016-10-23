package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*
import java.util.zip.ZipFile

class SendDataToVitaTask(vitaOrganizer: VitaOrganizer, val vpkFile: VpkFile) : VitaTask(vitaOrganizer) {
	fun performBase() : Boolean {

		status(Texts.format("STEP_SENDING_GAME", "id" to vpkFile.id))
		//val zip = ZipFile(entry.vpkFile)
		try {
			ZipFile(vpkFile.vpkFile).use { zip ->
				PsvitaDevice.uploadGame(vpkFile.id, zip, filter = { path -> FileRules.includeInData(path) }) { status ->
					//println("$status")
					status(Texts.format("STEP_SENDING_GAME_UPLOADING", "id" to vpkFile.id, "fileRange" to status.fileRange, "sizeRange" to status.sizeRange, "speed" to status.speedString))
				}
			}
			//statusLabel.text = "Processing game ${vitaGameCount + 1}/${vitaGameIds.size} ($gameId)..."
		} catch (e: Throwable) {
			error(e.toString())
			return false;
		}
		status(Texts.format("SENT_GAME_DATA", "id" to vpkFile.id))
		return true;
	}

	override fun perform() {
		if(performBase())
			status(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to vpkFile.id))
		else
			status("Failed to send ${vpkFile.id}")
	}
}