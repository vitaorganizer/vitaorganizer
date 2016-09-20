package com.soywiz.vitaorganizer.tasks

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

open class VitaTask(val vitaOrganizer: VitaOrganizer) {
	class Progress(var current: Long, var total: Long) {
		fun set(current: Long, total: Long) {
			this.current = current
			this.total = total
		}
	}

	val globalProgress = Progress(0L, 0L)
	val localProgress = Progress(0L, 0L)

	fun status(status: String) {
		SwingUtilities.invokeLater {
			vitaOrganizer.updateStatus(status)
		}
	}

	fun info(text: String) {
		SwingUtilities.invokeLater {
			JOptionPane.showMessageDialog(vitaOrganizer, text, Texts.format("INFORMATION"), JOptionPane.INFORMATION_MESSAGE)
		}
		status(text)
	}

	fun error(text: String) {
		SwingUtilities.invokeLater {
			JOptionPane.showMessageDialog(vitaOrganizer, text, "Error", JOptionPane.ERROR_MESSAGE)
		}
		status(text)
	}

	fun warn(title: String, text: String): Boolean {
		val result = JOptionPane.showConfirmDialog(vitaOrganizer, text, title, JOptionPane.YES_NO_OPTION)
		status(text)
		return (result == JOptionPane.YES_OPTION)
	}

	fun progress(current: Long, max: Long) {
		localProgress.set(current, max)
	}

	fun progress(current: Int, max: Int) {
		localProgress.set(current.toLong(), max.toLong())
	}

	open fun checkBeforeQueue() {
	}

	open fun perform() {
	}

	open fun cancel() {
	}
}