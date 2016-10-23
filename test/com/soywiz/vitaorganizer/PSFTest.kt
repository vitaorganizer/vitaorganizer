package com.soywiz.vitaorganizer

import com.soywiz.util.open2
import com.soywiz.vitaorganizer.ext.getResourceBytes
import com.soywiz.vitaorganizer.ext.getResourceStream2
import org.junit.Assert
import org.junit.Test

class PSFTest {
	@Test
	fun testLoadParamSfo() {
		Assert.assertEquals(
			mapOf(
				"APP_VER" to "00.95",
				"ATTRIBUTE" to 32768,
				"ATTRIBUTE2" to 0,
				"ATTRIBUTE_MINOR" to 16,
				"BOOT_FILE" to "",
				"CATEGORY" to "gd",
				"CONTENT_ID" to "",
				"EBOOT_APP_MEMSIZE" to 0,
				"EBOOT_ATTRIBUTE" to 0,
				"EBOOT_PHY_MEMSIZE" to 0,
				"LAREA_TYPE" to 0,
				"NP_COMMUNICATION_ID" to "",
				"PARENTAL_LEVEL" to 1,
				"PSP2_DISP_VER" to "00.000",
				"PSP2_SYSTEM_VER" to 0,
				"STITLE" to "VitaShell",
				"TITLE" to "VitaShell",
				"TITLE_ID" to "VITASHELL",
				"VERSION" to "00.00"
			),
			PSF.read(getResourceStream2("vitashell.param.sfo")!!)
		)
	}
}