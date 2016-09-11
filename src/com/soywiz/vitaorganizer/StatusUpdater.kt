package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.i18n.Text

interface StatusUpdater {
	fun updateStatus(status: String)
}
fun StatusUpdater.updateStatus(status: Text) = updateStatus(status.toString())
