package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.GameEntry
import com.soywiz.vitaorganizer.PsvitaDevice

class OneStepToVitaTask(val entry: GameEntry) : VitaTask() {
	val sendPromotingVpkTask = SendPromotingVpkToVitaTask(entry)
	val sendDataTask = SendDataToVitaTask(entry)

	override fun checkBeforeQueue() {
		sendPromotingVpkTask.checkBeforeQueue()
		sendDataTask.checkBeforeQueue()
	}

	override fun perform() {
		sendPromotingVpkTask.performBase()

		status("Promoting VPK (this could take a while)...")
		PsvitaDevice.promoteVpk(sendPromotingVpkTask.vpkPath)
		PsvitaDevice.removeFile(sendPromotingVpkTask.vpkPath)

		sendDataTask.performBase()

		info("Game ${entry.id} sent successfully")
	}
}