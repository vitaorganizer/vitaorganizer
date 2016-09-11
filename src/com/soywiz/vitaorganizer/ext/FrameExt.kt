package com.soywiz.vitaorganizer.ext

import javax.swing.JDialog
import javax.swing.JFrame

fun JFrame.showDialog(modal: JFrame) {

	val dialog = JDialog(this, modal.title, true)
	dialog.contentPane.add(modal.components.first())
	dialog.pack()
	dialog.setLocationRelativeTo(this)
	dialog.isVisible = true
	//frame.isEnabled = false

	//val frame2 = KeyValueViewerFrame(mapOf("a" to "b"))
	//frame2.pack()
	//frame2.setLocationRelativeTo(frame)
	//frame2.isVisible = true
}