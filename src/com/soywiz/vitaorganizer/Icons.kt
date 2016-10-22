package com.soywiz.vitaorganizer

import javax.swing.ImageIcon

object Icons {
	val OPEN_FOLDER = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/open_folder.png"))
	val INSTALL = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/install.png"))
	val REFRESH = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/refresh.png"))
	val UNKNOWN = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_unknown.png"))
	val CONNECTED = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_connected.png"))
	val DISCONNECTED = ImageIcon(VitaOrganizer::class.java.classLoader.getResource("com/soywiz/vitaorganizer/icons/icon_disconnected.png"))
}
