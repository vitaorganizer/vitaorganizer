package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.PsvitaDevice
import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.VpkFile

class OneStepToVitaTask(vitaOrganizer: VitaOrganizer, val vpkFile: VpkFile) : VitaTask(vitaOrganizer) {
	val sendPromotingVpkTask = SendPromotingVpkToVitaTask(vitaOrganizer, vpkFile)
	val sendDataTask = SendDataToVitaTask(vitaOrganizer, vpkFile)

	override fun checkBeforeQueue() {
		sendPromotingVpkTask.checkBeforeQueue()
		sendDataTask.checkBeforeQueue()
	}

	override fun perform() {
		if (!sendPromotingVpkTask.performBase()) {
			println("Uploading promoting VPK failed!")
			status("Uploading promoting VPK failed! Task aborted!")
			return
		}

		status(Texts.format("PROMOTING_VPK"))
		if (!PsvitaDevice.promoteVpk(sendPromotingVpkTask.vpkPath)) {
			status("Promoting failed! Task aborted!")
			return
		}
		PsvitaDevice.removeFile("/" + sendPromotingVpkTask.vpkPath)

		if (sendDataTask.performBase())
			info(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to vpkFile.id))
		else
			status("Failed to send data from ${vpkFile.id}")
	}
}