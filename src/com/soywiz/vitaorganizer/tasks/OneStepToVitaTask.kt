package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.GameEntry
import com.soywiz.vitaorganizer.PsvitaDevice
import com.soywiz.vitaorganizer.Texts

class OneStepToVitaTask(val entry: GameEntry) : VitaTask() {
	val sendPromotingVpkTask = SendPromotingVpkToVitaTask(entry)
	val sendDataTask = SendDataToVitaTask(entry)

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

		info(Texts.format("GAME_SENT_SUCCESSFULLY", "id" to entry.id))
	}
}