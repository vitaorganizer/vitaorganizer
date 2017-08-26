package com.soywiz.vitaorganizer.ext

import java.awt.Desktop
import java.net.URI
import java.net.URL

fun openWebpage(uri: URI) {
	val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
	if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
		try {
			desktop.browse(uri)
		} catch (e: Exception) {
			e.printStackTrace()
		}

	}
}

fun openWebpage(url: URL) = openWebpage(url.toURI())

fun openWebpage(url: String) = openWebpage(URL(url).toURI())

