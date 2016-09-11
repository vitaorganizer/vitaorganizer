package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.GameEntry
import com.soywiz.vitaorganizer.PsvitaDevice
import com.soywiz.vitaorganizer.VitaTaskQueue

class OneStepToVitaTask(val entry: GameEntry) : VitaTask() {
	val sendPromotingVpkTask = SendPromotingVpkToVitaTask(entry)
	val sendDataTask = SendDataToVitaTask(entry)

	override fun checkBeforeQueue() {
		sendPromotingVpkTask.checkBeforeQueue()
		sendDataTask.checkBeforeQueue()
	}

	override fun perform() {
		sendPromotingVpkTask.perform()

		updateStatus("Promoting VPK (this could take a while)...")
		PsvitaDevice.promoteVpk(sendPromotingVpkTask.vpkPath)
		PsvitaDevice.removeFile(sendPromotingVpkTask.vpkPath)

		sendDataTask.perform()
	}
}