package com.soywiz.vitaorganizer

interface StatusUpdater {
	fun updateStatus(status: String)
}

//fun StatusUpdater.updateStatus(status: Text) = updateStatus(status.toString())
