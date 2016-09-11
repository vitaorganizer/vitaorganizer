package com.soywiz.vitaorganizer.ext

import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage

fun Image.getScaledImage(w: Int, h: Int): Image {
	val resizedImg = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
	val g2 = resizedImg.createGraphics()

	g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
	g2.drawImage(this, 0, 0, w, h, null)
	g2.dispose()

	return resizedImg
}