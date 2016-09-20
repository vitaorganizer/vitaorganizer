package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.*

class OneStepToVitaTask(vitaOrganizer: VitaOrganizer, val vpkFile: VpkFile) : VitaTask(vitaOrganizer) {
	val sendPromotingVpkTask = SendPromotingVpkToVitaTask(vitaOrganizer, vpkFile)
	val sendDataTask = SendDataToVitaTask(vitaOrganizer, vpkFile)

	override fun checkBeforeQueue() {
		sendPromotingVpkTask.checkBeforeQueue()
		sendDataTask.checkBeforeQueue()
	}

	override fun perform() {
		sendPromotingVpkTask.performBase()

		status(Texts.format("PROMOTING_VPK"))
		PsvitaDevice.promoteVpk(sendPromotingVpkTask.vpkPath)
		PsvitaDevice.removeFile(sendPromotingVpkTask.vpkPath)

		sendDataTask.performBase()

		info(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to vpkFile.id))
	}
}