package com.soywiz.vitaorganizer.ext

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun JFrame.showDialog(modal: JFrame) {
	val dialog = JDialog(this, modal.title, true)
	dialog.contentPane.add(modal.components.first())
	dialog.pack()
	dialog.setLocationRelativeTo(this)
	dialog.isResizable = modal.isResizable
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

fun <T: JComponent> T.onClick(callback: () -> Unit): T {
	addMouseListener(object: MouseAdapter() {
		override fun mouseClicked(e: MouseEvent?) {
			callback()
		}
	})
	return this
}

fun JButton.action(callback: () -> Unit): JButton = onClick(callback)