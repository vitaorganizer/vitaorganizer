package com.soywiz.vitaorganizer.popups

import com.soywiz.vitaorganizer.Texts
import com.soywiz.vitaorganizer.VitaOrganizer
import com.soywiz.vitaorganizer.ext.onClick
import com.soywiz.vitaorganizer.ext.openWebpage
import java.awt.*
import javax.swing.*

class AboutFrame() : JFrame(Texts.format("ABOUT_TITLE")) {
	fun JLinkLabel(text: String, link: String, extra: String = ""): JLabel {
		return JLabel("""<html><a href="$link">$text</a>$extra</html>""").apply {
			cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
		}.onClick {
			openWebpage(link)
		}
	}

	fun Contibutor(text: String, link: String, role: String = ""): JComponent {
		return JLinkLabel(text, link, if (role.isNotEmpty()) " - $role" else "")
	}

	init {
		isResizable = false
		add(JPanel(BorderLayout()).apply {
			add(JLabel("VitaOrganizer ${VitaOrganizer.currentVersion}", SwingConstants.CENTER).apply {
				font = Font("Arial", Font.BOLD, 24)
			}, BorderLayout.NORTH)
			add(object : JPanel() {
				override fun getInsets(): Insets {
					return Insets(10, 10, 10, 10)
				}

				init {
					layout = BoxLayout(this, BoxLayout.Y_AXIS)

					add(JLabel(Texts.format("ABOUT_PROGRAMMING")))
					add(Contibutor("soywiz", "http://soywiz.com/"))
					add(Contibutor("luckylucks", "https://github.com/luckylucks/"))
					add(JLabel(Texts.format("ABOUT_TRANSLATIONS")))
					add(Contibutor("paweloszu", "https://github.com/paweloszu", "Polish"))
					add(Contibutor("RijjiResa", "https://github.com/RijjiResa", "Norwegian"))
					add(Contibutor("pvc1", "https://github.com/pvc1", "Russian"))
					add(Contibutor("Mdslino", "https://github.com/Mdslino", "Brazilian"))
					add(Contibutor("gordon0001", "https://github.com/gordon0001", "German"))
					add(Contibutor("charlyzard", "https://github.com/charlyzard", "Spanish"))
					add(Contibutor("anthologist", "https://github.com/anthologist", "Italian"))
					add(Contibutor("adeldk", "https://github.com/adeldk", "French"))
				}
			})
			preferredSize = Dimension(500, 400)
		})
	}
}