package com.soywiz.vitaorganizer.popups

import com.soywiz.vitaorganizer.PSF
import com.soywiz.vitaorganizer.ext.findFirstByClass
import com.soywiz.vitaorganizer.ext.getResourceStream2
import org.junit.Assert
import org.junit.Test
import javax.swing.JTable

class KeyValueViewerFrameTest {
	@Test
	fun test() {
		val frame = KeyValueViewerFrame("PSF", PSF.read(getResourceStream2("vitashell.param.sfo")!!))
		val table = frame.findFirstByClass<JTable>()!!
		Assert.assertEquals(19, table.rowCount)
		Assert.assertEquals(listOf("APP_VER", "00.95"), listOf(table.model.getValueAt(0, 0), table.model.getValueAt(0, 1)))
		Assert.assertEquals(listOf("ATTRIBUTE", "32768"), listOf(table.model.getValueAt(1, 0), table.model.getValueAt(1, 1)))
	}
}