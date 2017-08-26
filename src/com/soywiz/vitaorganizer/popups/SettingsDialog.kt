package com.soywiz.vitaorganizer.popups

import com.soywiz.vitaorganizer.Texts
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel

class SettingsDialog : JFrame(Texts.format(("MENU_OPTIONS"))) {

	init {
		add(JPanel(BorderLayout()).apply {
			preferredSize = Dimension(400, 400)
		})
	}
}
