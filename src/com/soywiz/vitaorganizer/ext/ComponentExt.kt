package com.soywiz.vitaorganizer.ext

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JMenuItem

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

fun JMenuItem.action(callback: () -> Unit): JMenuItem {
	addActionListener { callback() }
	return this
}

fun JButton.action(callback: () -> Unit): JButton {
	addMouseListener(object: MouseAdapter() {
		override fun mouseClicked(e: MouseEvent?) {
			callback()
		}
	})
	return this
}
