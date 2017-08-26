package com.soywiz.vitaorganizer.popups

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import java.awt.*
import javax.swing.*

class SettingsDialog : JFrame(Texts.format(("MENU_OPTIONS"))) {

	init {
		add(JPanel(BorderLayout()).apply {
			preferredSize = Dimension(400, 400)
		})
	}
}
