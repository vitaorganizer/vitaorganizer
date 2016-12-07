package com.soywiz.vitaorganizer

import com.soywiz.vitaorganizer.ext.setTemporarily
import org.junit.Assert
import org.junit.Test
import java.util.*

class FileSizeTest {
	@Test
	fun testDefaultFileSize() {

		if(VitaOrganizerSettings.unitBase == 10) {
			Assert.assertEquals("0 B", FileSize.toString(0L))
			Assert.assertEquals("1 kB", FileSize.toString(1000L))
			Assert.assertEquals("121 kB", FileSize.toString(121000L))
			Assert.assertEquals("11.8 MB", FileSize.toString(11800000L))
			Assert.assertEquals("11.498 GB", FileSize.toString(11498000000L))
			Assert.assertEquals("1.122833 TB", FileSize.toString(112283300000L))
		}
		else if(VitaOrganizerSettings.unitBase == 2){
			Assert.assertEquals("0 B", FileSize.toString(0L))
			Assert.assertEquals("1 KiB", FileSize.toString(1024L))
			Assert.assertEquals("121 KiB", FileSize.toString(123456L))
			Assert.assertEquals("11.8 MiB", FileSize.toString(12345678L))
			Assert.assertEquals("11.498 GiB", FileSize.toString(12345678901L))
			Assert.assertEquals("1.122833 TiB", FileSize.toString(1234567890123L))
		}
	}

	@Test fun testDefaultFileSizeEnglishLocale() = Locale.ENGLISH.setTemporarily { testDefaultFileSize() }
	@Test fun testDefaultFileSizeSpanishLocale() = Locale.forLanguageTag("es").setTemporarily { testDefaultFileSize() }
}
